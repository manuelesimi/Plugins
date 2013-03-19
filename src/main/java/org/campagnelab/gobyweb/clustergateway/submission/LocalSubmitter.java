package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.Task;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;
import org.campagnelab.gobyweb.filesets.protos.ReferenceInputListWriter;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Submitter for local task executions.
 * @author manuele
 */
public class LocalSubmitter implements Submitter {

    private static Logger logger = Logger.getLogger(LocalSubmitter.class);

    private static final String wrapperScript = "local_task_wrapper_script.sh";

    public LocalSubmitter() {}

    public Session newSession() { return  new Session();}

    /**
     * Submits local tasks
     * @param session
     * @param task
     * @throws Exception
     */
    public void submitTask(JobArea jobArea, Session session, Task task) throws Exception {
        final File taskLocalDir = new File(jobArea.getPath(), task.getTag());
        if (!taskLocalDir.exists())
              FileUtils.forceMkdir(taskLocalDir);

        //create protocol buffer for filesets
        ReferenceInputListWriter inputList = new ReferenceInputListWriter();
        inputList.setPushInfo(new File(session.targetAreaReferenceName).getAbsolutePath(),
                session.targetAreaOwner, new File(session.callerAreaReferenceName).getAbsolutePath(), session.callerAreaOwner);
        inputList.buildFileSetReferenceList(new File(session.targetAreaReferenceName).getAbsolutePath(), session.targetAreaOwner,
                task.getInputFileSets());

        FileUtils.copyFileToDirectory(inputList.serialize(), taskLocalDir);

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(wrapperScript);
        FileUtils.copyURLToFile(wrapperScriptURL, new File(taskLocalDir ,wrapperScript));

        //get the wrapper script
        URL constantsURL = getClass().getClassLoader().getResource(constantsTemplate);
        String constantsContent = IOUtils.toString(constantsURL);
        constantsContent = constantsContent.replaceAll("%%JOB_DIR%%", jobArea.getBasename(task.getTag()))
                .replaceAll("%%TAG%%", task.getTag());
        FileUtils.writeStringToFile(new File(taskLocalDir, constantsTemplate),constantsContent);

        //copy the task files
        for (File taskFile : task.getFiles()) {
            logger.info("Copying task file " + taskFile.getAbsolutePath());
            FileUtils.copyFileToDirectory(taskFile, taskLocalDir);
        }

        //copy all the resources' files in the local working dir
        for (Resource resource : task.getSourceConfig().requires) {
            logger.info("Resolving resource: " + resource.id);
            ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.id, resource.versionAtLeast, resource.versionExactly, resource.versionAtMost);
            for (PluginFile file : resourceConfig.files) {
                Files.copy(file.getLocalFile(), new File(taskLocalDir, file.getLocalFile().getName()));
            }
        }

        //give execute permission to task scripts
        grantExecutePermissions(taskLocalDir);

        //execute the task
        logger.info(String.format("Task %s: submitting to local cluster %s...", task.getTag(), taskLocalDir.getAbsolutePath()));
        execute(taskLocalDir);

    }

    /**
     * Grants execute permission to the scripts in the task directory
     * @param taskDir
     * @throws Exception
     */
    private void grantExecutePermissions(File taskDir) throws Exception {
        for (String file : new String[]{wrapperScript, "constants.sh", "script.sh"}) {
            ProcessBuilder builder = new ProcessBuilder("chmod", "777", String.format("%s%s%s",taskDir, File.separator, file));
            builder.directory( taskDir.getAbsoluteFile() );
            builder.redirectErrorStream(true);
            Process process =  builder.start();

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
     * @param taskDir
     * @throws IOException
     */
    private void execute(File taskDir) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(String.format("%s%s%s",taskDir, File.separator, wrapperScript) );
        builder.directory( taskDir.getAbsoluteFile() );
        builder.redirectErrorStream(true);
        Process process =  builder.start();
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
            logger.error("Task execution failed",e);
        }
    }
}