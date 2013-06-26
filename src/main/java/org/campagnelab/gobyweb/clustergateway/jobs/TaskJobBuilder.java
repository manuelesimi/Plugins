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
     * Adds aligner-specific settings to the job.
     * @param executableJob
     * @param commandLineOptions
     */
    @Override
    protected void customizeJob(ExecutableJob executableJob, final Map<String, String> commandLineOptions) {
        JobRuntimeEnvironment environment = executableJob.getEnvironment();
        environment.put("INITIAL_STATE", "task");

    }
}
