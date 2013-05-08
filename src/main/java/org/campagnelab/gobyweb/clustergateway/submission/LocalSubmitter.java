package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.clustergateway.jobs.ResourceJob;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Submitter for local task executions.
 *
 * @author manuele
 */
public class LocalSubmitter extends AbstractSubmitter implements Submitter {

    private static Logger logger = Logger.getLogger(LocalSubmitter.class);

    private static final String taskWrapperScript = "local_task_wrapper_script.sh";

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
     * @param job
     * @throws Exception
     */
    @Override
    public void submitJob(JobArea jobArea, Session session, ExecutableJob job) throws Exception {

        jobArea.createTag(job.getTag());
        //in the local submitter we directly access to the job area folder to avoid creating and then copying local files
        final File taskLocalDir = new File(jobArea.getBasename(job.getTag()));

        //prepare the protocol buffer with the job data
        File pbFile = this.createJobDataPB(session, job);
        FileUtils.copyFileToDirectory(pbFile, taskLocalDir);

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(taskWrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(taskLocalDir, taskWrapperScript));

        //write constants script
        FileUtils.writeStringToFile(new File(jobArea.getBasename(job.getTag()), constantsTemplate), writeConstants(jobArea, job));

        copyResourceFiles(job.getSourceConfig(), taskLocalDir);

        //give execute permission to task scripts
        jobArea.grantExecutePermissions(job.getTag(), new String[]{taskWrapperScript});

        //execute the task
        logger.info(String.format("Task %s: submitting to local cluster %s...", job.getTag(), taskLocalDir.getAbsolutePath()));
        logger.info("Output from the task : ");
        logger.info(jobArea.execute(job.getTag(),taskWrapperScript));
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

        FileUtils.writeStringToFile(new File(jobArea.getBasename(resourceJob.getTag()), constantsTemplate), writeConstants(jobArea, resourceJob));

        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);

        copyArtifactsPbRequests(resourceJob.getSourceConfig(), this.environmentScriptFilename, taskLocalDir);

        copyResourceFiles(registry.findByTypedId("GOBYWEB_SERVER_SIDE", ResourceConfig.class), taskLocalDir);

        copyResourceFiles(resourceJob.getSourceConfig(), taskLocalDir);

        //give execute permission to resourceJob scripts
        jobArea.grantExecutePermissions(resourceJob.getTag(), new String[]{resourceInstallWrapperScript});

        //execute the resourceJob
        logger.info(String.format("Resource %s: submitting to local cluster %s...", resourceJob.getTag(), taskLocalDir.getAbsolutePath()));
        Map<String, String> env = new HashMap<String, String>();
        env.put("JOB_DIR", taskLocalDir.getAbsolutePath());
        jobArea.execute(resourceJob.getTag(), resourceInstallWrapperScript,env);
    }

}