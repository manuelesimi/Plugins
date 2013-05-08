package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.clustergateway.jobs.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.jobs.Job;


import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Submitter for remote jobs.
 *
 * @author manuele
 */
public class RemoteSubmitter extends AbstractSubmitter implements Submitter {

    private static Logger logger = Logger.getLogger(RemoteSubmitter.class);

    private static final String wrapperScript = "oge_task_wrapper_script.sh";

    private String queue;

    public RemoteSubmitter(PluginRegistry registry, String queue) {
        super(registry);
        this.queue = queue;
    }

    public RemoteSubmitter(PluginRegistry registry) {
        super(registry);
    }

    public Session newSession() {
        return new Session();
    }

    /**
     * Submits a task instance for execution.
     *
     * @param jobArea the persistent area where task files will be placed for execution
     * @param session
     * @param job
     * @throws Exception
     */
    @Override
    public void submitJob(JobArea jobArea, Session session, ExecutableJob job) throws Exception {

        //create the temp dir with the submission files to move on the cluster
        File tempDir = Files.createTempDir();

        //prepare the protocol buffer with the job data
        File pbfile = this.createJobDataPB(session,job);
        Files.copy(pbfile, new File(tempDir, pbfile.getName()));

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(wrapperScript);
        String wrapperContent = IOUtils.toString(wrapperScriptURL);
        wrapperContent = wrapperContent.replace("%%QUEUE_NAME%%", this.queue);
        FileUtils.writeStringToFile(new File(tempDir, wrapperScript), wrapperContent);

        //get the wrapper script
        URL constantsURL = getClass().getClassLoader().getResource(constantsTemplate);
        String constantsContent = IOUtils.toString(constantsURL);
        constantsContent = constantsContent.replaceAll("%%JOB_DIR%%", jobArea.getBasename(job.getTag()))
                .replaceAll("%%TAG%%", job.getTag());
        FileUtils.writeStringToFile(new File(tempDir, constantsTemplate), constantsContent);

        copyResourceFiles(job.getSourceConfig(), tempDir);

        pushJobDir(tempDir,job,jobArea);

        //grant execute permissions to the task's scripts
        String[] binaryFiles = new String[]{"script.sh", constantsTemplate, wrapperScript};
        jobArea.grantExecutePermissions(job.getTag(), binaryFiles);

        //execute the task
        logger.info("Requesting job execution...");
        jobArea.execute(job.getTag(), wrapperScript);
        logger.info(String.format("The job is going to be executed in the following directory: %s", jobArea.getBasename(job.getTag())));

    }


    @Override
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resourceJob) throws Exception {
        //create the temp dir with the submission files to move on the cluster
        File tempDir = Files.createTempDir();
        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(resourceInstallWrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(tempDir, resourceInstallWrapperScript));

        FileUtils.writeStringToFile(new File(tempDir, constantsTemplate), writeConstants(jobArea, resourceJob));

        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);

        copyArtifactsPbRequests(resourceJob.getSourceConfig(), this.environmentScriptFilename, tempDir);

        copyResourceFiles(registry.findByTypedId("GOBYWEB_SERVER_SIDE", ResourceConfig.class), tempDir);

        copyResourceFiles(resourceJob.getSourceConfig(), tempDir);

        pushJobDir(tempDir,resourceJob,jobArea);

        //give execute permission to resourceJob scripts
        jobArea.grantExecutePermissions(resourceJob.getTag(), new String[]{resourceInstallWrapperScript});

        //execute the resourceJob
        logger.info(String.format("The job is going to be executed in the following directory: %s", jobArea.getBasename(resourceJob.getTag())));
        Map<String, String> env = new HashMap<String, String>();
        env.put("JOB_DIR", jobArea.getBasename(resourceJob.getTag()));
        jobArea.execute(resourceJob.getTag(), resourceInstallWrapperScript,env);
    }

    private void pushJobDir(File localDir, Job job, JobArea jobArea) throws Exception {
        //we need to rename the local temp dir with the tag of the task before we copy it on the cluster node
        File localWorkingDir = new File(localDir.getParentFile(), job.getTag());
        logger.trace(String.format("job %s: moving %s to %s", job.getTag(), localDir.getAbsolutePath(), localWorkingDir.getAbsolutePath()));
        if (localWorkingDir.exists())
            FileUtils.forceDelete(localWorkingDir);
        FileUtils.moveDirectory(localDir, localWorkingDir);
        //upload the entire folder in the job area
        logger.info("Submitting files for execution...");
        jobArea.push(job.getTag(), localWorkingDir);
        logger.info(String.format("The job has been successfully submitted to %s", jobArea.getReferenceName()));
        FileUtils.forceDeleteOnExit(localWorkingDir);
    }
}
