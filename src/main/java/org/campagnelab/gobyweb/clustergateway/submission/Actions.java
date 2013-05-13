package org.campagnelab.gobyweb.clustergateway.submission;

import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.*;

import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Execute actions requested through the command line for ClusterGateway.
 *
 * @author manuele
 */
final class Actions {

    private Submitter submitter;

    private String fileSetAreaReference;

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
     * @param fileSetAreaReference
     * @param jobArea
     * @param registry
     * @throws IOException if the creation of the folder where to store job results fails
     */
    protected Actions(Submitter submitter, String fileSetAreaReference, JobArea jobArea, PluginRegistry registry) throws IOException {
        this.registry = registry;
        this.fileSetAreaReference = fileSetAreaReference;
        this.jobArea = jobArea;
        this.submitter = submitter;
        if (!returnedJobFiles.exists())
            FileUtils.forceMkdir(returnedJobFiles);
    }


    public void submitJob(String id, Set<InputSlotValue> inputFilesets) throws Exception {

        //prepare the session for the submission
        Session session = submitter.newSession();
        prepareCallerSession(session, returnedJobFiles);
        session.targetAreaReferenceName = fileSetAreaReference;
        session.targetAreaOwner = jobArea.getOwner();

        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);

        //look for the source configuration
        ExecutableConfig config = registry.findByTypedId(id, ExecutableConfig.class);
        if (config != null) {
            this.submit(config, inputFilesets, session);
            return;
        } else {
            throw new IllegalArgumentException("Could not find an executable plugins with ID=" + id);
        }

    }

    /**
     * Submits a job for execution
     *
     * @param config        the source configuration
     * @param inputFilesets the input filesets
     * @param session       the session for the submitter
     * @throws Exception
     */
    private void submit(ExecutableConfig config, Set<InputSlotValue> inputFilesets, Session session) throws Exception {
        //create the job instance
        ExecutableJob job = new ExecutableJob(config);
        job.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to the job: " + job.getTag());
        //add the input filesets
        job.addInputSlotValues(inputFilesets);
        //submit the job
        submitter.submitJob(jobArea, session, job);
    }

    /**
     * Submits a resource for installation
     *
     * @param id      the resource id
     * @param version the resource version
     * @throws Exception
     */
    public void submitResourceInstall(String id, String version) throws Exception {
        //create the resourceInstance instance
        ResourceConfig config = DependencyResolver.resolveResource(id, version, version, version);
        if (config == null) {
            throw new IllegalArgumentException(String.format("Unable to locate resource with id=%s version=%s", id, version));
        }
        ResourceJob resourceInstance = new ResourceJob(config);
        resourceInstance.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to Task instance: " + resourceInstance.getTag());

        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);

        //prepare the session for the submission
        Session session = submitter.newSession();
        prepareCallerSession(session, returnedJobFiles);
        session.targetAreaReferenceName = fileSetAreaReference;
        session.targetAreaOwner = jobArea.getOwner();

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
        FileUtils.forceMkdir(resultsDir);
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
