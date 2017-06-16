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
import java.util.Arrays;
import java.util.Map;
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
        submitter.submitJob(jobArea, session, job);
    }


    /**
     * Prepares the session for the job execution.
     *
     * @throws Exception
     */
    private Session prepareJobSession() throws Exception {
        Session session = submitter.newSession();
        session.targetAreaReferenceName = fileSetAreaReference;
        session.targetAreaOwner = jobArea.getOwner();
        //create the directory for results
        FileUtils.forceMkdir(returnedJobFiles);
        if (jobArea.isLocal()) {
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
     * @param memory
     * @throws Exception
     */
    protected void submitAligner(AlignerConfig alignerConfig, Set<InputSlotValue> inputSlots, String genomeID,
                                 long chunkSize, Map<String, String> unclassifiedOptions, int memory) throws Exception {
        AlignerJobBuilder builder = new AlignerJobBuilder(alignerConfig, jobArea,
                fileSetAreaReference, jobArea.getOwner(), inputSlots);
        builder.setChunkSize(chunkSize);
        builder.setGenomeID(genomeID);
        builder.setContainerMemory(memory);
        if (!submitter.isLocal())
            submitter.setWrapperScripts("oge_job_script.sh","oge_job_script_legacy.sh", "common.sh");
        else
            throw new UnsupportedOperationException("Local submission for aligners is not supported yet");
        this.submitJob(builder.build(unclassifiedOptions), inputSlots);
    }

    /**
     * Submits an alignment analysis for execution.
     *  @param analysisConfig
     * @param inputSlots
     * @param groups_definitions group definition list, each definition in the form: Group_N=TAG,TAG342,TAG231...
     * @param comparison_pairs comparison pair list, each pair in the form "Group_1/Group_2"
     * @param unclassifiedOptions
     * @param memory
     */
    protected void submitAnalysis(AlignmentAnalysisConfig analysisConfig, Set<InputSlotValue> inputSlots, String[] groups_definitions,
                                  String[] comparison_pairs, Map<String, String> unclassifiedOptions, int memory)
            throws Exception {

        AlignmentAnalysisJobBuilder builder = new AlignmentAnalysisJobBuilder(analysisConfig, jobArea,
                fileSetAreaReference, jobArea.getOwner(), inputSlots);
        builder.setGroupDefinition(Arrays.asList(groups_definitions));
        builder.setComparisonPairs(Arrays.asList(comparison_pairs));
        builder.setContainerMemory(memory);
        if (!submitter.isLocal())
            submitter.setWrapperScripts("oge_job_script.sh","oge_job_script_legacy.sh", "common.sh");
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
     * @param memory
     * @throws Exception
     */
    protected void submitTask(TaskConfig taskConfig, Set<InputSlotValue> inputSlots,
                              Map<String, String> unclassifiedOptions, int memory) throws Exception {

        TaskJobBuilder builder = new TaskJobBuilder(taskConfig);
        builder.setContainerMemory(memory);
        if (submitter.isLocal())
            submitter.setWrapperScripts("local_task_wrapper_script.sh","local_task_wrapper_script_legacy.sh", "common.sh");
        else
            submitter.setWrapperScripts("oge_task_wrapper_script.sh", "oge_task_wrapper_script_legacy.sh", "common.sh");

        this.submitJob(builder.build(unclassifiedOptions), inputSlots);
    }

    /**
     * Submits a resource for installation.
     *
     * @param config      the resource
     * @throws Exception
     */
    protected void submitResourceInstall( ResourceConfig config) throws Exception {

        ResourceJob resourceInstance = new ResourceJob(config);
        //resourceInstance.setTag(ICBStringUtils.generateRandomString());
        logger.debug("Tag assigned to Task instance: " + resourceInstance.getTag());

        //prepare the session for the submission
        Session session = prepareJobSession();
        submitter.setWrapperScripts("resource_install_wrapper_script.sh", "common.sh");

        //submit the resourceInstance
        submitter.submitResourceInstall(jobArea, session, resourceInstance);
    }

}
