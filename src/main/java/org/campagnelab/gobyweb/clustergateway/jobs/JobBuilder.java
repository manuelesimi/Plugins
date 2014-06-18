package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.plugins.xml.executables.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base Job builder
 *
 * @author manuele
 */
public abstract class JobBuilder {

    private final ExecutableConfig executableConfig;
    private final CommonJobConfiguration jobConfiguration;

    protected JobBuilder(ExecutableConfig executableConfig, CommonJobConfiguration jobConfiguration) {
        this.executableConfig = executableConfig;
        this.jobConfiguration = jobConfiguration;
    }

    /**
     * Parses plugin runtime requirements.
     *
     * For each need, adds a variable to replacements for each scope in pluginConfig runtime requirements.
     * Variables are called PLUGINS_NEED_${scope}*
     *
     * For each artifacts element found, adds a variable that states if a scope requires artifacts installation or not.
     */
    private Map<String, Object> buildResourceRequirements() {

        Map<String, Object> requirementsByScope = new HashMap<String, Object>();
        List<Need> needs = executableConfig.getRuntime().needs();
        for (Need need : needs) {
            // if key is present, format as key=value,
            // otherwise, just write value to PLUGIN_NEED constant.
            String needAsString = (need.key != null && (!need.key.equalsIgnoreCase(""))) ?
                    String.format("%s=%s", need.key,need.value): need.value;
            String key = String.format("PLUGIN_NEED_%s", need.scope);
            if (requirementsByScope.containsKey(key)) {
                requirementsByScope.put(key, requirementsByScope.get(key)+","+needAsString);
            } else {
                requirementsByScope.put(key, needAsString);
            }
        }
        List<Artifacts> artifactsList = executableConfig.getRuntime().artifacts();
        for (Artifacts artifacts: artifactsList) {
            String key = String.format("PLUGIN_ARTIFACTS_%s", artifacts.scope);
            requirementsByScope.put(key, Boolean.toString(artifacts.required));
        }
        return requirementsByScope;
    }

    /**
     * Builds the job.
     * @param commandLineOptions  options specified by the user
     * @return the job
     * @throws IOException
     */
    public ExecutableJob build(final Map<String, String> commandLineOptions) throws IOException {
        ExecutableJob executableJob = new ExecutableJob(executableConfig, jobConfiguration);
        //default memory settings (can be overridden by subclasses)
        executableJob.setMemoryInGigs(8);
        this.manageConfigOptions(commandLineOptions);
        executableJob.getEnvironment().putAll(this.buildResourceRequirements());
        this.customizeJob(executableJob, commandLineOptions);
        //user options have priority and eventually overwrites the others
        executableJob.getEnvironment().putAll(commandLineOptions);
        return executableJob;
    }

    /**
     * Validates the values passed on the command line with the expected value for
     * the options defined in the plugin configuration. If the value is accepted, the key is decorated with the
     * plugin's prefix.
     * @param commandLineOptions
     * @throws IOException
     */
    private void manageConfigOptions(final Map<String, String> commandLineOptions) throws IOException {
        Map<String, String> decoratedCommandLineOptions = new HashMap<String, String>();
        Iterator<Map.Entry<String,String>> it = commandLineOptions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,String> userOption = it.next();
            Option configOption = executableConfig.getOption(userOption.getKey());
            if ( configOption != null) { //this option comes from the plugin configuration
                OptionError error = configOption.validateOptionValue(userOption.getValue());
                if (error != null) {
                    throw new IOException(String.format("Invalid value for option %s: %s",userOption.getKey(), error.message));
                }
                else {
                    //decorated the key with the prefix
                    decoratedCommandLineOptions.put(this.getVariablePrefix() + userOption.getKey(),userOption.getValue());
                    //remove the original option from the source map
                    it.remove();
                }
            }
        }
        commandLineOptions.putAll(decoratedCommandLineOptions);
    }

    /**
     * Returns the prefix to prepend to plugin's variables reported in constants.sh.
     *
     * @return the prefix
     */
    protected abstract String getVariablePrefix();

    /**
     * Allows subclasses to customize the job settings.
     * @param executableJob
     * @param commandLineOptions
     */
    protected abstract void customizeJob(ExecutableJob executableJob, final Map<String, String> commandLineOptions) throws IOException;

}
