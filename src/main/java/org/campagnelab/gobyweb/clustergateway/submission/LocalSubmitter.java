package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.data.TaskJob;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;
import org.campagnelab.gobyweb.filesets.protos.ReferenceInputListWriter;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.*;
import java.net.URL;
import java.util.Scanner;

/**
 * Submitter for local task executions.
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
        final File taskLocalDir = new File(jobArea.getPath(), taskJob.getTag());
        if (!taskLocalDir.exists())
            FileUtils.forceMkdir(taskLocalDir);

        //create protocol buffer for filesets
        ReferenceInputListWriter inputList = new ReferenceInputListWriter();
        inputList.setPushInfo(new File(session.targetAreaReferenceName).getAbsolutePath(),
                session.targetAreaOwner, new File(session.callerAreaReferenceName).getAbsolutePath(), session.callerAreaOwner);
        inputList.buildFileSetReferenceList(new File(session.targetAreaReferenceName).getAbsolutePath(), session.targetAreaOwner,
                taskJob.getInputFileSets());

        FileUtils.copyFileToDirectory(inputList.serialize(), taskLocalDir);

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(taskWrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(taskLocalDir, taskWrapperScript));

        writeConstants(jobArea, taskJob, taskLocalDir);


        copyResourceFiles(taskJob.getSourceConfig(), taskLocalDir);

        //give execute permission to task scripts
        grantExecutePermissions(taskWrapperScript, taskLocalDir);

        //execute the task
        logger.info(String.format("Task %s: submitting to local cluster %s...", taskJob.getTag(), taskLocalDir.getAbsolutePath()));
        execute(taskWrapperScript, taskLocalDir);

    }




    /**
     * Submits a resourceJob installation job.
     *
     * @param session
     * @param resourceJob
     * @throws Exception
     */
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resourceJob) throws Exception {
        final File taskLocalDir = new File(jobArea.getPath(), resourceJob.getTag());
        if (!taskLocalDir.exists())
            FileUtils.forceMkdir(taskLocalDir);


        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(resourceInstallWrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(taskLocalDir, resourceInstallWrapperScript));

        writeConstants(jobArea, resourceJob, taskLocalDir);


        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);

        copyArtifactsPbRequests(resourceJob.getSourceConfig(), null, taskLocalDir);

        copyResourceFiles(registry.findByTypedId("GOBYWEB_SERVER_SIDE", ResourceConfig.class),
                taskLocalDir);
        copyResourceFiles(resourceJob.getSourceConfig(), taskLocalDir);

        //give execute permission to resourceJob scripts
        grantExecutePermissions(resourceInstallWrapperScript, taskLocalDir);

        //execute the resourceJob
        logger.info(String.format("Task %s: submitting to local cluster %s...", resourceJob.getTag(), taskLocalDir.getAbsolutePath()));
        execute(resourceInstallWrapperScript, taskLocalDir);

    }

    /**
     * Grants execute permission to the scripts in the task directory
     *
     * @param taskDir
     * @throws Exception
     */
    private void grantExecutePermissions(String wrapperScript, File taskDir) throws Exception {
        for (String file : new String[]{wrapperScript, "constants.sh", "script.sh"}) {
            ProcessBuilder builder = new ProcessBuilder("chmod", "777", String.format("%s%s%s", taskDir, File.separator, file));
            builder.directory(taskDir.getAbsoluteFile());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try {
                process.waitFor();
                if (process.exitValue() == 0)
                    logger.info("Execute permissions successfully granted to " + file);
                else
                    logger.info("Failed to grant execute permissions to " + file);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes the task in the directory.
     *
     * @param taskDir
     * @throws IOException
     */
    private void execute(String wrapperScript, File taskDir) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(String.format("%s%s%s", taskDir, File.separator, wrapperScript));
        builder.directory(taskDir.getAbsoluteFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        Scanner s = new Scanner(process.getInputStream());
        StringBuilder text = new StringBuilder();
        while (s.hasNextLine()) {
            text.append(s.nextLine());
            text.append("\n");
        }
        s.close();
        logger.info("Task output: \n" + text);
        try {
            process.waitFor();
            if (process.exitValue() != 0)
                logger.error("Task execution failed");
        } catch (InterruptedException e) {
            logger.error("Task execution failed", e);
        }
    }
}