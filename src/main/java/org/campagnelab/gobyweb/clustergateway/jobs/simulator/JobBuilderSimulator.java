package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import static org.campagnelab.gobyweb.clustergateway.jobs.simulator.Option.*;

import java.io.*;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Simulate the environment that a job will see at execution time.
 *
 * @author manuele
 */
public class JobBuilderSimulator {

    private final ExecutableConfig executableConfig;

    private final ResourceConfig resourceConfig;

    private final PluginRegistry registry;

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
        //resource-related options
        for (Resource resourceRef : executableConfig.getRequiredResources()) {
            this.populateResourceOptions(resourceRef,env);
        }
    }

}
