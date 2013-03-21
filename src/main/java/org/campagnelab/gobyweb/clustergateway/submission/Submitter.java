package org.campagnelab.gobyweb.clustergateway.submission;

import org.campagnelab.gobyweb.clustergateway.data.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.data.TaskJob;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;

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
     * Submits a task instance for execution.
     *
     * @param jobArea the persistent area where task files will be placed for execution
     * @param session
     * @param taskJob
     * @throws Exception
     */
    public void submitTask(JobArea jobArea, Session session, TaskJob taskJob) throws Exception;

    /**
     * Submits a resource installation job.
     *
     * @param session
     * @param resource
     * @throws Exception
     */
    public void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resource) throws Exception;

    void setSubmissionHostname(String submissionHostname);

    void setEnvironmentScript(String environmentScriptFilename);

    void setRemoteArtifactRepositoryPath(String artifactRepositoryPath);
}
