package org.campagnelab.gobyweb.clustergateway.submission;

import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.clustergateway.jobs.ResourceJob;
import org.campagnelab.gobyweb.io.JobArea;

/**
 * Model the behavior of a submitter in the Cluster Gateway.
 *
 * @author manuele
 */
public interface Submitter {

    public static final String constantsTemplate = "constants.sh";

    /**
     * Returns a session that can be used to submit tasks to the submitter.
     *
     * @return a session
     */
    public Session newSession();

    /**
     * Submits a job for execution.
     *
     * @param jobArea the persistent area where task files will be placed for execution
     * @param session
     * @param job
     * @throws Exception
     */
    public void submitJob(JobArea jobArea, Session session, ExecutableJob job) throws Exception;

    /**
     * Submits a resource installation job.
     *
     * @param session
     * @param resource
     * @throws Exception
     */
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resource) throws Exception;

    public void setSubmissionHostname(String submissionHostname);

    public void setEnvironmentScript(String environmentScriptFilename);

    public void setRemoteArtifactRepositoryPath(String artifactRepositoryPath);

    public void setFileSetAreaReference(String fileSetAreaReference);
    /**
     * Sets the tag to assign to the job being submitted.
     * @param jobTag
     */
    public void assignTagToJob(String jobTag);

    /**
     * The script to run for executing the job, or a pattern (i.e., oge_script*.sh) identifying several scripts
     * needed for execution. All scripts matched by the pattern will be written to the job directory after expansion
     * of %variables%. The very first script matched by the pattern will be executed.
     * @param wrapperScripts
     */
    public void setWrapperScripts(String ... wrapperScripts);

    /**
     * Checks if this is a local or remote submitter.
     * @return
     */
    public boolean isLocal();

    /**
     * Sets the paths from where the script wrappers have to be loaded.
     * @param paths
     */
    public void setWrappersPaths(String[] paths);

    /**
     * Sets the technology of script wrappers to use.
     * @param technology the type of technology (docker, singularity, etc.)
     * @param memory
     * @param name the container name
     */
    public void setContainerTechnology(String technology, int memory, String ... name);

}
