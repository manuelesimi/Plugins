package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.io.JobArea;

import java.util.Set;

/**
 * Job submission settings common to all executable jobs.
 *
 * @author manuele
 */
public class CommonJobConfiguration {

    private JobArea jobArea;
    private String filesetAreaReference;
    private String submissionFilesetAreaReference;
    private boolean useSubmissionFSA = false;
    private String owner;
    private Set<InputSlotValue> inputSlots;
    private String brokerHostname;
    private int brokerPort = 0;
    private boolean useBroker = false;
    private boolean hasCloudAccess = false;
    private String cloudId;
    private String cloudSecret;
    private String cloudToken;
    private String targetBucket;

    public boolean useBroker() {
        return useBroker;
    }

    public void setUseBroker(boolean useBroker) {
        this.useBroker = useBroker;
    }


    public String getTargetBucket() {
        return targetBucket;
    }

    public void setTargetBucket(String targetBucket) {
        this.targetBucket = targetBucket;
    }

    public JobArea getJobArea() {
        return jobArea;
    }

    public void setJobArea(JobArea jobArea) {
        this.jobArea = jobArea;
    }

    public String getFilesetAreaReference() {
        return filesetAreaReference;
    }

    public void setFilesetAreaReference(String filesetAreaReference) {
        this.filesetAreaReference = filesetAreaReference;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String user) {
        this.owner = user;
    }

    public Set<InputSlotValue> getInputSlots() {
        return inputSlots;
    }

    public void setInputSlots(Set<InputSlotValue> inputSlots) {
        this.inputSlots = inputSlots;
    }

    public String getBrokerHostname() {
        return brokerHostname;
    }

    public void setBrokerHostname(String brokerHostname) {
        this.brokerHostname = brokerHostname;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(int brokerPort) {
        this.brokerPort = brokerPort;
    }

    public String getSubmissionFilesetAreaReference() {
        return useSubmissionFSA? submissionFilesetAreaReference : filesetAreaReference;
    }

    public void setSubmissionFilesetAreaReference(String submissionFilesetAreaReference) {
        useSubmissionFSA = true;
        this.submissionFilesetAreaReference = submissionFilesetAreaReference;
    }

    public boolean hasCloudAccess() {
        return hasCloudAccess;
    }

    public void setCloudAccess(boolean hasCloudAccess) {
        this.hasCloudAccess = hasCloudAccess;
    }

    public String getCloudId() {
        return cloudId;
    }

    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public String getCloudSecret() {
        return cloudSecret;
    }

    public void setCloudSecret(String cloudSecret) {
        this.cloudSecret = cloudSecret;
    }

    public String getCloudToken() {
        return cloudToken;
    }

    public void setCloudToken(String cloudToken) {
        this.cloudToken = cloudToken;
    }
}
