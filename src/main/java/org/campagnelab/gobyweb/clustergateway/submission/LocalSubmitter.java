package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.data.TaskJob;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.*;
import java.net.URL;

/**
 * Submitter for local job executions.
 *
 * @author manuele
 */
public class LocalSubmitter extends AbstractSubmitter implements Submitter {

    private static Logger logger = Logger.getLogger(LocalSubmitter.class);

    private static final String taskWrapperScript = "local_task_wrapper_script.sh";
    private static final String resourceInstallWrapperScript = "local_resource_install_wrapper_script.sh";

    public LocalSubmitter(PluginRegistry registry) {
        super(registry);
    }

    public Session newSession() {
        return new Session();
    }

    /**
     * Submits local tasks
     *
     * @param session
     * @param taskJob
     * @throws Exception
     */
    public void submitTask(JobArea jobArea, Session session, TaskJob taskJob) throws Exception {

        jobArea.createTag(taskJob.getTag());
        //in the local submitter we directly access to the job area folder to avoid creating and then copying local files
        final File taskLocalDir = new File(jobArea.getBasename(taskJob.getTag()));

        File pbFile = this.createJobDataPB(session, taskJob);

        FileUtils.copyFileToDirectory(pbFile, taskLocalDir);

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(taskWrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(taskLocalDir, taskWrapperScript));

        writeConstants(jobArea, taskJob);

        copyResourceFiles(taskJob.getSourceConfig(), taskLocalDir);

        //give execute permission to task scripts
        jobArea.grantExecutePermissions(taskJob.getTag(), new String[]{taskWrapperScript});

        //execute the task
        logger.info(String.format("Task %s: submitting to local cluster %s...", taskJob.getTag(), taskLocalDir.getAbsolutePath()));
        logger.info("Output from the task : ");
        logger.info(jobArea.execute(taskJob.getTag(),taskWrapperScript));
    }


    /**
     * Submits a resourceJob installation job.
     *
     * @param session
     * @param resourceJob
     * @throws Exception
     */
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resourceJob) throws Exception {

        jobArea.createTag(resourceJob.getTag());
        final File taskLocalDir = new File(jobArea.getBasename(resourceJob.getTag()));

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(resourceInstallWrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(jobArea.getBasename(resourceJob.getTag()), resourceInstallWrapperScript));

        writeConstants(jobArea, resourceJob);

        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);

        copyArtifactsPbRequests(resourceJob.getSourceConfig(), null, taskLocalDir);

        copyResourceFiles(registry.findByTypedId("GOBYWEB_SERVER_SIDE", ResourceConfig.class), taskLocalDir);

        copyResourceFiles(resourceJob.getSourceConfig(), taskLocalDir);

        //give execute permission to resourceJob scripts
        jobArea.grantExecutePermissions(resourceJob.getTag(), new String[]{resourceInstallWrapperScript});

        //execute the resourceJob
        logger.info(String.format("Task %s: submitting to local cluster %s...", resourceJob.getTag(), taskLocalDir.getAbsolutePath()));
        jobArea.execute(resourceJob.getTag(), resourceInstallWrapperScript);

    }

}