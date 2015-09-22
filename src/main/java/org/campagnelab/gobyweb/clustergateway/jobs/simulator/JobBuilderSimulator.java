package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import org.campagnelab.gobyweb.clustergateway.submission.SubmissionRequest;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.Need;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import static org.campagnelab.gobyweb.clustergateway.jobs.simulator.Option.*;

import java.io.*;
import java.util.*;

/**
 * Simulate the environment that a job will see at execution time.
 *
 * @author manuele
 */
public class JobBuilderSimulator {

    private final ExecutableConfig executableConfig;

    private final ResourceConfig resourceConfig;

    private final PluginRegistry registry;
    private SubmissionRequest.ArtifactInfoMap artifactsAttributes;

    /**
     *
     * @param config the plugin configuration from which the job would be created.
     * @param registry
     */
    public JobBuilderSimulator(ResourceConfig config, PluginRegistry registry) {
        this.resourceConfig = config;
        this.registry = registry;
        this.executableConfig = null;
    }

    /**
     *
     * @param config the plugin configuration from which the job would be created.
     * @param registry
     */
    public JobBuilderSimulator(ExecutableConfig config, PluginRegistry registry) {
        this.executableConfig = config;
        this.registry = registry;
        this.resourceConfig = null;
    }

    /**
     * Tries to recreate the job submission environment.
     * @return the list of variables visible to the job.
     * @throws IOException
     */
    public SortedSet<Option> simulateAutoOptions() throws IOException {
        SortedSet<Option> env = new TreeSet<Option>();
        if (executableConfig !=null)
            this.populateJobOptions(executableConfig, env);
        else  {
            Resource resource = new Resource();
            resource.id = this.resourceConfig.getId();
            resource.versionExactly = this.resourceConfig.getVersion();
            this.populateResourceOptions(resource, env);
        }
        return env;
    }


    /**
     * Detects the available options for a resource plugin.
     * @param resource
     * @param env
     * @throws IOException
     */
    private void populateResourceOptions(Resource resource, SortedSet<Option> env) throws IOException {
       ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.id,
                null, resource.versionExactly);
       if (resourceConfig == null)
           return;
       for (Resource ref : resourceConfig.requires)
           populateResourceOptions(ref, env);

        for (PluginFile file : resourceConfig.getFiles()) {
            String name = String.format("RESOURCES_%s_%s", resourceConfig.getId(), file.id);
            String value = String.format("${JOB_DIR}/%s",file.filename);
            env.add(new Option(name, value, file.isDirectory? OptionKind.DIRECTORY : OptionKind.FILE));
        }

        ArtifactInstallerSimulator.populateArtifactsOptions(resourceConfig, env, this.artifactsAttributes);
    }

    private OptionKind detectOptionKind(org.campagnelab.gobyweb.plugins.xml.executables.Option option) {
        switch (option.type) {
            case BOOLEAN: return OptionKind.BOOLEAN;
            case DOUBLE: case INTEGER: return OptionKind.NUMERIC;
            case STRING: return OptionKind.STRING;
            default: {
                return OptionKind.STRING;
            }
        }
    }

    /**
     * Detects the available options for an executable plugin.
     * @param executableConfig
     * @param env
     * @throws IOException
     */
    private void populateJobOptions(ExecutableConfig executableConfig, SortedSet<Option> env) throws IOException {
        List<org.campagnelab.gobyweb.plugins.xml.executables.Option> optionsToProcess = executableConfig.options();
        for (org.campagnelab.gobyweb.plugins.xml.executables.Option option : optionsToProcess) {
            String pluginId = Plugins.scriptImportedFrom(executableConfig);
            String key;
            if (pluginId != null) {
                ExecutableConfig fromPlugin = registry.findByTypedId(pluginId, ExecutableConfig.class);
                // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID, where plugin refers to the plugin we imported the script from:
                key = String.format("PLUGINS_%s_%s_%s", fromPlugin.getHumanReadableConfigType(), fromPlugin.getId(), option.id);
                //if there is any auto-formattable option, all_other_plugins is available
                if (option.autoFormat) {
                    env.add(new Option(String.format("PLUGINS_%s_%s_ALL_OTHER_OPTIONS",
                            fromPlugin.getHumanReadableConfigType(),fromPlugin.getId()), "no-value", OptionKind.STRING));
                }
            } else {
                // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID
                key = String.format("PLUGINS_%s_%s_%s",executableConfig.getHumanReadableConfigType(),executableConfig.getId(), option.id);
                //if there is any auto-formattable option, all_other_plugins is available
                if (option.autoFormat) {
                    env.add(new Option(String.format("PLUGINS_%s_%s_ALL_OTHER_OPTIONS",
                            executableConfig.getHumanReadableConfigType(),executableConfig.getId()), "no-value", OptionKind.STRING));
                }
            }
            env.add(new Option(key, "no-value", detectOptionKind(option)));
        }
        //plugin files options
        for (PluginFile file : executableConfig.getFiles()) {
            // write options in the format  ${PLUGINS_ TYPE _ plugin-id _ FILES _ file-id}
            String key = String.format("PLUGINS_%s_%s_FILES_%s",
                    executableConfig.getHumanReadableConfigType(), executableConfig.getId(), file.id);
            String value = String.format("${JOB_DIR}/%s",file.filename);
            env.add(new Option(key, value, file.isDirectory? OptionKind.DIRECTORY : OptionKind.FILE));
        }

        //plugin needs
        this.populateNeedsOptions(executableConfig,env);

        //resource-related options
        for (Resource resourceRef : executableConfig.getRequiredResources()) {
            this.populateResourceOptions(resourceRef,env);
        }

        //extra-options coming from the SDK or input slots
        this.populateJobDefaultOptions(executableConfig, env);
        ArtifactInstallerSimulator.populateArtifactsOptions(executableConfig, env, artifactsAttributes);
    }

    /**
     * For each need, adds a variable to replacements for each scope in pluginConfig runtime requirements.
     * Variables are called PLUGINS_NEED_${scope}*
     */
    private void populateNeedsOptions(ExecutableConfig executableConfig, SortedSet<Option> env) {
        Map<String, String> requirementsByScope = new HashMap<String, String>();
        List<Need> needs = executableConfig.getRuntime().needs();
        //PLUGIN_NEED_ALIGN="excl=false,h_vmem=6g,virtual_free=6g"
        for (Need need : needs) {
            // if key is present, format as key=value,
            // otherwise, just write value to PLUGIN_NEED constant.
            String needAsString = (need.key != null && (!need.key.equalsIgnoreCase(""))) ?
                    String.format("%s=%s", need.key, need.value) : need.value;
            String key = String.format("PLUGIN_NEED_%s", need.scope);
            if (requirementsByScope.containsKey(key)) {
                requirementsByScope.put(key, requirementsByScope.get(key) + "," + needAsString);
            } else {
                requirementsByScope.put(key, needAsString);
            }
        }
        for (Map.Entry<String,String> option : requirementsByScope.entrySet()) {
           env.add(new Option(option.getKey(),option.getValue(),OptionKind.STRING));
        }
    }

    /**
     * Adds options generated by the SDK at job submission time.
     * @param executableConfig
     * @param env
     */
    private void populateJobDefaultOptions(ExecutableConfig executableConfig, SortedSet<Option> env) {
        //common options
        env.add(new Option("TAG", null, OptionKind.STRING));
        env.add(new Option("GOBY_DIR", null, OptionKind.DIRECTORY));
        env.add(new Option("FILESET_COMMAND", null, OptionKind.STRING));
        //plugin-specific options
        if ((executableConfig.getClass().isAssignableFrom(AlignerConfig.class)))
            env.addAll(AlignerDefaultOptions.get());
        else if ((executableConfig.getClass().isAssignableFrom(AlignmentAnalysisConfig.class)))
            env.addAll(AlignmentAnalysisDefaultOptions.get());
        else if ((executableConfig.getClass().isAssignableFrom(TaskConfig.class)))
            env.addAll(TaskDefaultOptions.get());
    }

    public void setArtifactsAttributes(SubmissionRequest.ArtifactInfoMap artifactsAttributes) {
        this.artifactsAttributes = artifactsAttributes;
    }
}
