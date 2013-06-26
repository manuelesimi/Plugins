package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.clustergateway.jobs.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.jobs.Job;


import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.File;
import java.io.IOException;
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

    public RemoteSubmitter(PluginRegistry registry, String queue) throws IOException {
        super(registry);
        this.queue = queue;
    }

    public RemoteSubmitter(PluginRegistry registry) throws IOException {
        super(registry);
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

        //complete the replacements map with the information available in the submitter
        this.completeJobEnvironment(job, jobArea.getBasename(job.getTag()));

        //prepare the protocol buffer with the job data
        File pbfile = this.createJobDataPB(session,job);
        Files.copy(pbfile, new File(tempDir, pbfile.getName()));

        //fill the wrapper script and copy it on the temp dir
        this.copyWrapperScript(job, tempDir);

        VariableHelper helper = new VariableHelper();
        helper.writeVariables(new File(tempDir, constantsTemplate), job.getEnvironment());

        copyResourceFiles(job.getSourceConfig(), tempDir);

        copyAutoOptions(job.getSourceConfig(), tempDir, job.getEnvironment());

        copyArtifactsPbRequests(job.getSourceConfig(), this.environmentScriptFilename, tempDir);

        //run pre-deployment scripts, if any
        runPreDeploymentScripts(job, tempDir);

        pushJobDir(tempDir,job,jobArea);

        //grant execute permissions to the task's scripts
        String[] binaryFiles = new String[]{"groovy", this.wrapperScript, "*"};
        jobArea.grantExecutePermissions(job.getTag(), binaryFiles);

        //execute the task
        logger.info("Requesting job execution...");
        logger.info("Output from the submission process:");
        jobArea.execute(job.getTag(), wrapperScript);
        logger.info(String.format("The job will be executed in the Job Area at %s/%s", jobArea.toString(),
                job.getTag()));

    }

    @Override
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resourceJob) throws Exception {
        //create the temp dir with the submission files to move on the cluster
        File tempDir = Files.createTempDir();
        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(this.wrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(tempDir, this.wrapperScript));

        FileUtils.writeStringToFile(new File(tempDir, constantsTemplate), writeConstants(jobArea, resourceJob));

        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);

        copyArtifactsPbRequests(resourceJob.getSourceConfig(), this.environmentScriptFilename, tempDir);

        copyResourceFiles(registry.findByTypedId("GOBYWEB_SERVER_SIDE", ResourceConfig.class), tempDir);

        copyResourceFiles(resourceJob.getSourceConfig(), tempDir);

        File autoOptions = helper.generateAutoOptionsFile(new ResourceJobWrapper(resourceJob.getSourceConfig()));
        FileUtils.moveFile(autoOptions, new File(FilenameUtils.concat(tempDir.getAbsolutePath(), "auto-options.sh")));

        pushJobDir(tempDir,resourceJob,jobArea);

        //give execute permission to resourceJob script and anything at top level (needed for resource files)
        jobArea.grantExecutePermissions(resourceJob.getTag(), new String[]{this.wrapperScript, "*"});

        //execute the resourceJob
        logger.info(String.format("The job will be executed in the Job Area at %s/%s)", jobArea.toString(),
                resourceJob.getTag()));
        Map<String, String> env = new HashMap<String, String>();
        env.put("JOB_DIR", jobArea.getBasename(resourceJob.getTag()));
        jobArea.execute(resourceJob.getTag(), this.wrapperScript,env);
    }

    /**
     * Checks if this is a local or remote submitter
     *
     * @return
     */
    @Override
    public boolean isLocal() {
        return false;
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
        FileUtils.forceDeleteOnExit(localWorkingDir);
    }
}
