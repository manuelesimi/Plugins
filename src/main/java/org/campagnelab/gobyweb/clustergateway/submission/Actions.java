package org.campagnelab.gobyweb.clustergateway.submission;

import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.InputSlotValue;
import org.campagnelab.gobyweb.clustergateway.jobs.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.jobs.TaskJob;

import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.campagnelab.gobyweb.plugins.xml.Config;
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
        this.submitter=submitter;
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

        //detect the type of executable plugin we have to submit
        Config config = registry.findByTypedId(id, TaskConfig.class);
        if (config != null) {
           this.submitTask((TaskConfig)config, inputFilesets, session);
           return;
        }
        config = registry.findByTypedId(id, AlignerConfig.class);
        if (config != null) {
            this.submitAligner((AlignerConfig) config, inputFilesets, session);
            return;
        }

        config = registry.findByTypedId(id, AlignmentAnalysisConfig.class);
        if (config != null) {
            this.submitAlignmentAnalysis((AlignmentAnalysisConfig)config, inputFilesets, session);
            return;
        }

        if (config==null) {
            throw new IllegalArgumentException("Could not find an executable plugins with ID="+id);
        }

    }

    /**
     * Submits a task as a Job for execution
     * @param config the task configuration
     * @param inputFilesets the input filesets
     * @param session the session for the submitter
     * @throws Exception
     */
    private void submitTask(TaskConfig config, Set<InputSlotValue> inputFilesets, Session session) throws Exception{
        //create the task instance
        TaskJob taskJob = new TaskJob(config);
        taskJob.setTag(ICBStringUtils.generateRandomString());
        taskJob.addInputSlotValues(inputFilesets);
        logger.debug("Tag assigned to the Task instance: " + taskJob.getTag());
        //add the input filesets
        taskJob.addInputSlotValues(inputFilesets);
        //submit the task
        submitter.submitTask(jobArea, session, taskJob);
    }

    /**
     * Submits an aligner as a Job for execution
     * @param config the aligner configuration
     * @param inputFilesets the input filesets
     * @param session the session for the submitter
     * @throws Exception
     */
    private void submitAligner(AlignerConfig config, Set<InputSlotValue> inputFilesets, Session session) throws Exception{
          throw new UnsupportedOperationException("Aligners cannot be submitted yet");
    }

    /**
     * Submits an alignment analysis as a Job for execution
     * @param config the analysis configuration
     * @param inputFilesets the input filesets
     * @param session the session for the submitter
     * @throws Exception
     */
    private void submitAlignmentAnalysis(AlignmentAnalysisConfig config, Set<InputSlotValue> inputFilesets, Session session) throws Exception{
         throw new UnsupportedOperationException("Alignment Analyses cannot be submitted yet");
    }

    /**
     * Submits a resource for installation
     * @param id the resource id
     * @param version the resource version
     * @throws Exception
     */
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
