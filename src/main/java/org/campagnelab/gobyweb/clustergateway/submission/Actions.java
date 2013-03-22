package org.campagnelab.gobyweb.clustergateway.submission;

import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.data.TaskJob;

import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
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

    private Submitter submitter;

    private FileSetArea fileSetArea;

    private JobArea jobArea;

    private PluginRegistry registry;

    /**
     * The directory where the Cluster Gateway stores results of job executions
     */
    private static final File returnedJobFiles = new File(System.getProperty("user.home") + "/.clustergateway/RETURNED_JOB_FILES");

    private static Logger logger = Logger.getLogger(Actions.class);

    private Actions(Submitter submitter) {
        this.submitter = submitter;
    }

    /**
     * Creates a new Actions object.
     *
     * @param fileSetArea
     * @param jobArea
     * @param registry
     * @throws IOException if the creation of the folder where to store job results fails
     */
    protected Actions(Submitter submitter, FileSetArea fileSetArea, JobArea jobArea, PluginRegistry registry) throws IOException {
        this.registry = registry;
        this.fileSetArea = fileSetArea;
        this.jobArea = jobArea;
        this.submitter=submitter;
        if (!returnedJobFiles.exists())
            FileUtils.forceMkdir(returnedJobFiles);
    }


    public void submitTask(String id, String[] inputFilesets) throws Exception {
        //create the task instance
        TaskConfig config = registry.findByTypedId(id, TaskConfig.class);
        if (config==null) {
            throw new IllegalArgumentException("Could not find task with ID="+id);
        }
        TaskJob taskJob = new TaskJob(config);
        taskJob.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to Task instance: " + taskJob.getTag());

        //add the input filesets
        logger.debug("Input filesets: " + Arrays.toString(inputFilesets));
        for (String inFileset : inputFilesets)
            taskJob.addInputFileSet(inFileset);

        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);

        //prepare the session for the submission
        Session session = submitter.newSession();
        prepareCallerSession(session, returnedJobFiles);
        session.targetAreaReferenceName = fileSetArea.getReferenceName();
        session.targetAreaOwner = fileSetArea.getOwner();

        //submit the task
        submitter.submitTask(jobArea, session, taskJob);
    }

    public void submitResourceInstall(String id, String version) throws Exception {
        //create the resourceInstance instance

        ResourceConfig config = DependencyResolver.resolveResource(id, version, version, version);
        ResourceJob resourceInstance = new ResourceJob(config);
        resourceInstance.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to Task instance: " + resourceInstance.getTag());


        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);

        //prepare the session for the submission
        Session session = submitter.newSession();
        prepareCallerSession(session, returnedJobFiles);
        session.targetAreaReferenceName = fileSetArea.getReferenceName();
        session.targetAreaOwner = fileSetArea.getOwner();

        //submit the resourceInstance
        submitter.submitResourceInstall(jobArea, session, resourceInstance);
    }

    /**
     * Populates the session with the information needed by the task to send back information
     *
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
                throw new Exception("failed to get the local hostname", e);
            }
        }
        session.callerAreaOwner = System.getProperty("user.name");
    }
}
