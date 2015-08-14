package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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

import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.SERVER_SIDE_TOOL;

/**
 * Submitter for local task executions.
 *
 * @author manuele
 */
public class LocalSubmitter extends AbstractSubmitter implements Submitter {

    private static Logger logger = Logger.getLogger(LocalSubmitter.class);

    AutoOptionsFileHelper autoOptionsHelper = new AutoOptionsFileHelper(registry);


    public LocalSubmitter(PluginRegistry registry) throws IOException {
        super(registry);
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
        job.setTag(this.jobTag);
        job.setOwner(jobArea.getOwner());
        jobArea.createTag(job.getTag());
        //in the local submitter we directly access to the job area folder to avoid creating and then copying local files
        final File jobLocalDir = new File(jobArea.getBasename(job.getTag()));

        //complete the replacements map with the information available in the submitter
        this.completeJobEnvironment(job, jobArea.getBasename(job.getTag()));

        //prepare the protocol buffer with the job data
        File pbFile = this.createJobDataPB(session, job);
        FileUtils.copyFileToDirectory(pbFile, jobLocalDir);

        //copy the wrapper script in the execution dir
        this.copyWrapperScript(job, jobLocalDir);

        //write constants script
        VariableHelper helper = new VariableHelper();
        helper.writeVariables(new File(jobLocalDir, constantsTemplate), job.getEnvironment());

        copyResourceFiles(job.getSourceConfig(), jobLocalDir);

        copyAutoOptions(job.getSourceConfig(), jobLocalDir, job.getEnvironment());

        copyArtifactsPbRequests(job.getSourceConfig(), this.environmentScriptFilename, jobLocalDir);

        //run pre-deployment scripts, if any
        runPreDeploymentScripts(job, jobLocalDir);

        //give execute permission to task scripts
        jobArea.grantExecutePermissions(job.getTag(), new String[]{this.wrapperScript});

        //execute the task
        logger.info(String.format("Submitting job to local cluster at %s %s", job.getTag(),
                jobLocalDir.getAbsolutePath()));
        Map<String, String> env = new HashMap<String, String>();
        env.put("JOB_DIR", jobLocalDir.getAbsolutePath());
        env.put("PATH", System.getenv("PATH"));
        logger.info("Output from the submission process:");
        logger.info(jobArea.execute(job.getTag(),this.wrapperScript,env));
    }


    /**
     * Submits a resourceJob installation job.
     *
     * @param session
     * @param resourceJob
     * @throws Exception
     */
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resourceJob) throws Exception {
        resourceJob.setTag(this.jobTag);

        jobArea.createTag(resourceJob.getTag());
        final File taskLocalDir = new File(jobArea.getBasename(resourceJob.getTag()));

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(this.wrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(jobArea.getBasename(resourceJob.getTag()), this.wrapperScript));

        FileUtils.writeStringToFile(new File(jobArea.getBasename(resourceJob.getTag()), constantsTemplate), writeConstants(jobArea, resourceJob));

        copyArtifactsPbRequests(resourceJob.getSourceConfig(), this.environmentScriptFilename, taskLocalDir);

        copyResourceFiles(registry.findByTypedIdAndVersion(SERVER_SIDE_TOOL[0], SERVER_SIDE_TOOL[1], ResourceConfig.class), taskLocalDir);

        copyResourceFiles(resourceJob.getSourceConfig(), taskLocalDir);
        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);

        File autoOptions = helper.generateAutoOptionsFile(new ResourceJobWrapper(resourceJob.getSourceConfig()));
        FileUtils.moveFile(autoOptions, new File(FilenameUtils.concat(taskLocalDir.getAbsolutePath(), "auto-options.sh")));
        //give execute permission to resourceJob scripts
        String[] binaryFiles = new String[]{"groovy", this.wrapperScript, "auto-options.sh", "constants.sh"};
        jobArea.grantExecutePermissions(resourceJob.getTag(), binaryFiles);

        //execute the resourceJob
        logger.info(String.format("Resource %s: submitting to local cluster at %s...", resourceJob.getTag(), taskLocalDir.getAbsolutePath()));
        Map<String, String> env = new HashMap<String, String>();
        env.put("JOB_DIR", taskLocalDir.getAbsolutePath());
        env.put("PATH", System.getenv("PATH"));
        jobArea.execute(resourceJob.getTag(), this.wrapperScript, env);
    }


    /**
     * Checks if this is a local or remote submitter
     *
     * @return
     */
    @Override
    public boolean isLocal() {
        return true;
    }


}