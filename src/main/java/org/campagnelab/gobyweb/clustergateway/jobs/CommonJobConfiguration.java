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
    private String owner;
    private Set<InputSlotValue> inputSlots;
    private String brokerHostname;
    private int brokerPort;

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
}
