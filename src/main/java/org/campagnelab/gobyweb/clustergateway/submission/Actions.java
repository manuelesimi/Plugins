package org.campagnelab.gobyweb.clustergateway.submission;

import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.Task;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;

import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Execute actions requested through the command line for ClusterGateway.
 *
 * @author manuele
 */
final class Actions {


    private FileSetArea fileSetArea;

    private JobArea jobArea;

    private PluginRegistry registry;

    /**The directory where the Cluster Gateway stores results of job executions */
    private static final File returnedJobFiles = new File (System.getProperty( "user.home" ) + "/.clustergateway/RETURNED_JOB_FILES");

    private static Logger logger = Logger.getLogger(Actions.class);

    private Actions() {}

   /**
    * Creates a new Actions object.
    * @param fileSetArea
    * @param jobArea
    * @param registry
    * @throws IOException if the creation of the folder where to store job results fails
    */
    protected Actions(FileSetArea fileSetArea, JobArea jobArea, PluginRegistry registry) throws IOException {
        this.registry = registry;
        this.fileSetArea = fileSetArea;
        this.jobArea = jobArea;
        if (!returnedJobFiles.exists())
            FileUtils.forceMkdir(returnedJobFiles);
    }

    /**
     * Submits the task to the cluster.
     * @param queue the queue to use
     * @param id the id of the task configuration
     * @param inputFilesets the list of tags identifying the input filesets
     */
    protected void submitRemoteTask(String queue, String id, String[] inputFilesets) throws Exception {
        this.submitTask(new RemoteSubmitter(queue),id,inputFilesets);
    }

    /**
     * Submits the task to the local machine.
     * @param id the id of the task configuration
     * @param inputFilesets the list of tags identifying the input filesets
     */
    protected void submitLocalTask(String id, String[] inputFilesets) throws Exception  {
        this.submitTask(new LocalSubmitter(),id,inputFilesets);
    }

    private void submitTask(Submitter submitter, String id, String[] inputFilesets) throws Exception  {
        //create the task instance
        TaskConfig config = registry.findByTypedId(id, TaskConfig.class);
        Task task = new Task(config);
        task.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to Task instance: " + task.getTag());

        //add the input filesets
        logger.debug("Input filesets: " + Arrays.toString(inputFilesets));
        for (String inFileset : inputFilesets)
                task.addInputFileSet(inFileset);

        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);

        //prepare the session for the submission
        Session session = submitter.newSession();
        prepareCallerSession(session, returnedJobFiles);
        session.targetAreaReferenceName = fileSetArea.getReferenceName();
        session.targetAreaOwner = fileSetArea.getOwner();

        //submit the task
        submitter.submitTask(jobArea,session,task);
    }

    /**
     * Populates the session with the information needed by the task to send back information
     * @param session
     * @param resultsDir the local folder where results will be stored
     * @throws Exception
     */
    private void prepareCallerSession(Session session, File resultsDir) throws Exception {
        if (jobArea.isLocal()) {
           //the job is executed locally, it just needs a local reference to the results directory
            session.callerAreaReferenceName = resultsDir.getAbsolutePath();
        } else {
            //the job needs to contact the caller via ssh
            try {
                session.callerAreaReferenceName =
                        String.format("%s@%s:%s",
                                System.getProperty("user.name"),
                                java.net.InetAddress.getLocalHost().getHostName(),
                                resultsDir.getAbsolutePath());
            } catch (UnknownHostException e) {
                throw new Exception("failed to get the local hostname",e);
            }
        }
        session.callerAreaOwner = System.getProperty("user.name");
    }
}
