package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.*;

import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;

import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Execute actions requested through the command line for ClusterGateway.
 *
 * @author manuele
 */
final class Actions {

    private Submitter submitter;

    private PluginRegistry registry;

    private CommonJobConfiguration jobConfiguration;

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
     * @param submissionFileSetAreaReference
     * @param jobArea
     * @param registry
     * @throws IOException if the creation of the folder where to store job results fails
     */
    protected Actions(Submitter submitter, String fileSetAreaReference, String submissionFileSetAreaReference,
                      JobArea jobArea, PluginRegistry registry) throws IOException {
        this.registry = registry;
        this.jobConfiguration = new CommonJobConfiguration();
        this.jobConfiguration.setFilesetAreaReference(fileSetAreaReference);
        if (submissionFileSetAreaReference != null && !submissionFileSetAreaReference.equals(""))
            this.jobConfiguration.setSubmissionFilesetAreaReference(submissionFileSetAreaReference);
        this.jobConfiguration.setJobArea(jobArea);
        this.jobConfiguration.setOwner(jobArea.getOwner());
        this.jobConfiguration.setUseBroker(false);
        this.submitter = submitter;
        if (!returnedJobFiles.exists())
            FileUtils.forceMkdir(returnedJobFiles);
    }

    /**
     * Add broker configuration.
     *
     * @param brokerHostname
     * @param brokerPort
     */
    protected void setBroker(String brokerHostname, int brokerPort) {
        this.jobConfiguration.setBrokerHostname(brokerHostname);
        this.jobConfiguration.setBrokerPort(brokerPort);
        this.jobConfiguration.setUseBroker(true);
    }

    /**
     * Configure the instance to access to the cloud storage for fetching/pushing job results.
     * @param cloudId
     * @param cloudSecret
     */
    protected void configureCloudAccess(String cloudId, String cloudSecret) {
        this.jobConfiguration.setCloudAccess(true);
        this.jobConfiguration.setCloudId(cloudId);
        this.jobConfiguration.setCloudSecret(cloudSecret);
    }

    /**
     * Submits a job for execution
     *
     * @param job           the job to submit
     * @param inputFilesets the input filesets
     * @throws Exception
     */
    private void submitJob(ExecutableJob job, Set<InputSlotValue> inputFilesets) throws Exception {
        //prepare the session for the submission
        Session session = this.prepareJobSession();
        //job.setTag(submitter.j);
        logger.debug("Tag assigned to the job: " + job.getTag());
        //add the input filesets
        job.addInputSlotValues(inputFilesets);
        //submit the job
        submitter.submitJob(this.jobConfiguration.getJobArea(), session, job);
    }


    /**
     * Prepares the session for the job execution.
     *
     * @throws Exception
     */
    private Session prepareJobSession() throws Exception {
        Session session = submitter.newSession();
        session.targetAreaReferenceName = this.jobConfiguration.getFilesetAreaReference();
        session.targetAreaOwner = this.jobConfiguration.getOwner();
        if (this.jobConfiguration.hasCloudAccess())
            session.cloudConnection = new Session.GoogleCloudConnection(this.jobConfiguration.getCloudId(),
                this.jobConfiguration.getCloudSecret());
        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);
        if (this.jobConfiguration.getJobArea().isLocal()) {
            //the job is executed locally, it just needs a local reference to the results directory
            session.callerAreaReferenceName = returnedJobFiles.getAbsolutePath();
        } else {
            //the job needs to contact the caller via ssh
            try {
                session.callerAreaReferenceName =
                        String.format("%s@%s:%s",
                                System.getProperty("user.name"),
                                java.net.InetAddress.getLocalHost().getHostName(),
                                returnedJobFiles.getAbsolutePath());
            } catch (UnknownHostException e) {
                throw new Exception("failed to get the local hostname", e);
            }
        }
        session.callerAreaOwner = System.getProperty("user.name");
        return session;
    }

    /**
     * Submits an aligner for execution.
     *
     * @param alignerConfig
     * @param inputSlots
     * @param genomeID
     * @param chunkSize
     * @param unclassifiedOptions
     * @throws Exception
     */
    protected void submitAligner(AlignerConfig alignerConfig, Set<InputSlotValue> inputSlots, String genomeID,
                                 long chunkSize, Map<String, String> unclassifiedOptions) throws Exception {

        jobConfiguration.setInputSlots(inputSlots);
        AlignerJobBuilder builder = new AlignerJobBuilder(alignerConfig, jobConfiguration);
        builder.setChunkSize(chunkSize);
        builder.setGenomeID(genomeID);
        if (!submitter.isLocal())
            submitter.setWrapperScripts("oge_job_script.sh","oge_job_script_legacy.sh");
        else
            throw new UnsupportedOperationException("Local submission for aligners is not supported yet");
        this.submitJob(builder.build(unclassifiedOptions), inputSlots);
    }

    /**
     * Submits an alignment analysis for execution.
     *
     * @param analysisConfig
     * @param inputSlots
     * @param groups_definitions group definition list, each definition in the form: Group_N=TAG,TAG342,TAG231...
     * @param comparison_pairs comparison pair list, each pair in the form "Group_1/Group_2"
     * @param unclassifiedOptions
     */
    protected void submitAnalysis(AlignmentAnalysisConfig analysisConfig, Set<InputSlotValue> inputSlots, String[] groups_definitions,
                                  String[] comparison_pairs, Map<String, String> unclassifiedOptions)
            throws Exception {
        jobConfiguration.setInputSlots(inputSlots);
        AlignmentAnalysisJobBuilder builder = new AlignmentAnalysisJobBuilder(analysisConfig, jobConfiguration);
        builder.setGroupDefinition(Arrays.asList(groups_definitions));
        builder.setComparisonPairs(Arrays.asList(comparison_pairs));
        if (!submitter.isLocal())
            submitter.setWrapperScripts("oge_job_script.sh","oge_job_script_legacy.sh");
        else
            throw new UnsupportedOperationException("Local submission for aligners is not supported yet");
        this.submitJob(builder.build(unclassifiedOptions), inputSlots);
    }

    /**
     * Submits a task for execution
     *
     *
     * @param taskConfig         the plugin configuration
     * @param inputSlots the input filesets
     * @param unclassifiedOptions additional options defined by the user
     * @throws Exception
     */
    protected void submitTask(TaskConfig taskConfig, Set<InputSlotValue> inputSlots,
                              Map<String, String> unclassifiedOptions) throws Exception {

        jobConfiguration.setInputSlots(inputSlots);
        TaskJobBuilder builder = new TaskJobBuilder(taskConfig, jobConfiguration);

        if (submitter.isLocal())
            submitter.setWrapperScripts("local_task_wrapper_script.sh");
        else
            submitter.setWrapperScripts("oge_task_wrapper_script.sh");

        this.submitJob(builder.build(unclassifiedOptions), inputSlots);
    }

    /**
     * Submits a resource for installation.
     *
     * @param config      the resource
     * @param artifactsAttributes
     * @throws Exception
     */
    protected void submitResourceInstall(ResourceConfig config, SubmissionRequest.ArtifactInfoMap artifactsAttributes) throws Exception {
        List<ResourceConfig> singleton = new ArrayList<>();
        singleton.add(config);
        this.submitMultipleResourceInstall(singleton,artifactsAttributes);
    }

    /**
     * Submits a resource for installation.
     *
     * @param config      the resource
     * @param artifactsAttributes
     * @throws Exception
     */
    protected void submitMultipleResourceInstall(List<ResourceConfig> config, SubmissionRequest.ArtifactInfoMap artifactsAttributes) throws Exception {

        ResourceJob resourceInstance = new ResourceJob(config);
        if (artifactsAttributes.getResources().size() > 0)
            resourceInstance.setAttributes(artifactsAttributes);
        //resourceInstance.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to Task instance: " + resourceInstance.getTag());

        //prepare the session for the submission
        Session session = prepareJobSession();
        submitter.setWrapperScripts("resource_install_wrapper_script.sh");

        //submit the resourceInstance
        submitter.submitResourceInstall(jobConfiguration.getJobArea(), session, resourceInstance);
    }

}
