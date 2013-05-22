package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.Need;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base Job builder
 *
 * @author manuele
 */
public abstract class JobBuilder {

    private final ExecutableConfig executableConfig;

    protected JobBuilder(ExecutableConfig executableConfig) {
        this.executableConfig = executableConfig;
    }

    /**
     * For each need, adds a variable to replacements for each scope in pluginConfig runtime requirements.
     * Variables are called PLUGINS_NEED_${scope}*
     */
    protected Map<String, Object>  buildResourceRequirements() {

        Map<String, Object> requirementsByScope = new HashMap<String, Object>();
        List<Need> needs = executableConfig.getRuntime().needs();
        //PLUGIN_NEED_ALIGN="excl=false,h_vmem=6g,virtual_free=6g"
        for (Need need : needs) {
            // if key is present, format as key=value,
            // otherwise, just write value to PLUGIN_NEED constant.
            String needAsString = (need.key != null && (!need.key.equalsIgnoreCase(""))) ?
                    String.format("%s=%s", need.key,need.value): need.value;
            String key = "%" + String.format("PLUGIN_NEED_%s", need.scope) +"%";
            if (requirementsByScope.containsKey(key)) {
                requirementsByScope.put(key, requirementsByScope.get(key)+","+needAsString);
            } else {
                requirementsByScope.put(key, needAsString);
            }
        }
        return requirementsByScope;
    }


    public ExecutableJob build() throws IOException {
        ExecutableJob executableJob = new ExecutableJob(executableConfig);
        executableJob.addReplacements(this.createAdditionalReplacementMap());
        executableJob.addReplacements(this.buildResourceRequirements());
        //default memory settings (can be overridden by subclasses)
        executableJob.setMemoryInGigs(8);
        executableJob.setMemoryOverheadInGigs(2);
        this.addCustomSettings(executableJob);
        return executableJob;
    }

    /**
     * Allows subclasses to add custom settings to the job
     * @param executableJob
     */
    protected abstract void addCustomSettings(ExecutableJob executableJob);

    /**
     * Allows subclasses to create an additional replacements map
     * @return
     */
    protected abstract Map<String, Object> createAdditionalReplacementMap() throws IOException;
}
