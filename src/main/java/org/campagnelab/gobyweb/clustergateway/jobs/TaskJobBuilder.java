package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for task jobs
 * @author manuele
 */
public class TaskJobBuilder extends JobBuilder {

    private final TaskConfig taskConfig;

    public TaskJobBuilder(TaskConfig taskConfig) {
        super(taskConfig);
        this.taskConfig = taskConfig;
    }

    /**
     * Allows subclasses to add extra settings to the job
     *
     * @param executableJob
     */
    @Override
    protected void addCustomSettings(ExecutableJob executableJob) {
        //nothing to add
    }

    /**
     * Allows subclasses to create an additional replacements map
     *
     * @return
     */
    @Override
    protected Map<String, Object> createAdditionalReplacementMap() throws IOException {
        Map<String, Object> replacements = new HashMap<String, Object>();
        replacements.put("%INITIAL_STATE%", "task");

        return replacements;
    }
}
