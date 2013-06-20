package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAPResult;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

/**
 * Prepare a job request for a task.
 *
 * @author manuele
 */
class TaskSubmissionRequest extends SubmissionRequest {

    private TaskConfig taskConfig;

    protected TaskSubmissionRequest(TaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        this.executableConfig = taskConfig;
    }

    @Override
    protected int submit(JSAPResult config, Actions actions) throws Exception {
        if (taskConfig.isDisabled())
            throw new Exception(String.format("Task %s is currently disabled", taskConfig.getId()));
        actions.submitTask(taskConfig, this.getInputSlots(), this.getUnclassifiedOptions());
        return 0;
    }
}
