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

    /**
     *
     * @param taskConfig the source task config
     * @param jobConfiguration the job submission settings
     */
    public TaskJobBuilder(TaskConfig taskConfig, CommonJobConfiguration jobConfiguration) {
        super(taskConfig,jobConfiguration);
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
        environment.put("PLUGIN_ID", this.taskConfig.getId());
        environment.put("PLUGIN_VERSION", this.taskConfig.getVersion());
    }

    @Override
    protected String getVariablePrefix() {
        return String.format("PLUGINS_TASK_%s_",this.taskConfig.getId());
    }
}
