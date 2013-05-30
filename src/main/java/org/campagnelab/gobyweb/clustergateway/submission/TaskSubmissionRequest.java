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
    }

    @Override
    protected int submit(JSAPResult config, Actions actions) throws Exception {
        actions.submitTask(taskConfig.getId(), this.getInputSlots(), this.getUnclassifiedOptions());
        return 0;
    }
}
