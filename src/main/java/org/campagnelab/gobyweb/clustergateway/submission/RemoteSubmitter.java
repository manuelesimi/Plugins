package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;
import org.campagnelab.gobyweb.clustergateway.data.Task;

import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;
import org.campagnelab.gobyweb.filesets.protos.ReferenceInputListWriter;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.File;
import java.net.URL;

/**
 * Submitter for cluster nodes.
 * @author manuele
 */
public class RemoteSubmitter implements Submitter {

    private static Logger logger = Logger.getLogger(RemoteSubmitter.class);

    private static final String wrapperScript = "oge_task_wrapper_script.sh";

    private String queue;

    public RemoteSubmitter(String queue) {this.queue = queue;}

    public Session newSession() { return  new Session();}

    /**
    * Submits a task instance for execution.
    * @param jobArea the persistent area where task files will be placed for execution
    * @param session
    * @param task
    * @throws Exception
    */
    public void submitTask(JobArea jobArea, Session session, Task task) throws Exception {

        //create the temp dir with the submission files to move on the cluster
        File tempDir = Files.createTempDir();

        //create protocol buffer for filesets
        ReferenceInputListWriter inputList = new ReferenceInputListWriter();
        inputList.setPushInfo(session.targetAreaReferenceName,
                session.targetAreaOwner,session.callerAreaReferenceName,session.callerAreaOwner);
        inputList.buildFileSetReferenceList(session.targetAreaReferenceName, session.targetAreaOwner,
                task.getInputFileSets());

        File pbfile =  inputList.serialize();
        Files.copy(pbfile, new File(tempDir, pbfile.getName()));

        //get the wrapper script
        URL wrapperScriptURL = getClass().getClassLoader().getResource(wrapperScript);
        String wrapperContent = IOUtils.toString(wrapperScriptURL);
        wrapperContent = wrapperContent.replace("%%QUEUE_NAME%%", this.queue);
        FileUtils.writeStringToFile(new File(tempDir, wrapperScript),wrapperContent);

        //get the wrapper script
        URL constantsURL = getClass().getClassLoader().getResource(constantsTemplate);
        String constantsContent = IOUtils.toString(constantsURL);
        constantsContent = constantsContent.replaceAll("%%JOB_DIR%%", jobArea.getBasename(task.getTag()))
                .replaceAll("%%TAG%%", task.getTag());
        FileUtils.writeStringToFile(new File(tempDir, constantsTemplate),constantsContent);

        //copy the task files
        for (File taskFile : task.getFiles())
            Files.copy(taskFile, new File(tempDir, taskFile.getName()));


        //copy all the resources' files in the local working dir
        for (Resource resource : task.getSourceConfig().requires) {
            logger.info("Resolving resource: " + resource.id);
            ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.id, resource.versionAtLeast, resource.versionExactly, resource.versionAtMost);
            for (PluginFile file : resourceConfig.files) {
                Files.copy(file.getLocalFile(), new File(tempDir, file.getLocalFile().getName()));
            }
        }

        //we need to rename the local temp dir with the tag of the task before to copy it on the cluster node
        File localWorkingDir = new File(tempDir.getParentFile(), task.getTag());
        logger.trace(String.format("Task %s: moving %s to %s", task.getTag(), tempDir.getAbsolutePath(), localWorkingDir.getAbsolutePath()));
        if (localWorkingDir.exists())
            FileUtils.forceDelete(localWorkingDir);
        FileUtils.moveDirectory(tempDir,localWorkingDir);
        logger.info("Task dir: " + jobArea.getBasename(task.getTag()) );
        //upload the entire folder in the job area
        logger.info("Task submitting files for execution...");
        jobArea.upload(task.getTag(),localWorkingDir);

        //grant execute permissions to the task's scripts
        String[] binaryFiles = new String[] {"script.sh", constantsTemplate, wrapperScript};
        jobArea.grantExecutePermissions(task.getTag(), binaryFiles);

        //execute the task
        logger.info("Requesting task execution...");
        jobArea.execute(task.getTag(), wrapperScript);
        FileUtils.forceDeleteOnExit(localWorkingDir);
    }

}
