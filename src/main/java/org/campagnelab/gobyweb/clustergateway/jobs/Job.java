package org.campagnelab.gobyweb.clustergateway.jobs;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Base Job configuration for submission to the ClusterGateway.
 *
 * @author manuele
 */
public abstract class Job {

    protected static final Logger logger = Logger.getLogger(Job.class);

    String id;

    String tag;

    String owner_id ="";

    Date creation_date;

    String description = "";

    /**
     * Folder where the data belonging this object are stored
     */
    String basename;

    /**
     * Files belonging to the task that need to be copied on the cluster before its execution
     */
    private List<File> files = new ArrayList<File>();

    private JobRuntimeEnvironment environment = new JobRuntimeEnvironment();

    private int memoryInGigs;

    private boolean parallel = false;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getOwnerId() {
        return owner_id;
    }

    public void setOwner(String owner_id) {
        this.owner_id = owner_id;
    }

    public Date getCreationDate() {
        return creation_date;
    }

    public void setCreationDate(Date creation_date) {
        this.creation_date = creation_date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    /**
     * Gets the list of files belonging the instance.
     *
     * @return
     */
    public List<File> getFiles() {
        return this.files;
    }

    /**
     * Adds a new file belonging the instance. The file will be copied in the task execution folder.
     *
     * @param file
     */
    public void addFile(File file) {
        this.files.add(file);
    }


    /**
     * Adds the additional environment settings to the Job.
     * @param settings
     */
    protected void addToEnvironment(Map<String, Object> settings) {
        this.environment.putAll(settings);
    }

    public JobRuntimeEnvironment getEnvironment() {
        return environment;
    }

    public void setMemoryInGigs(int memoryInGigs) {
        this.memoryInGigs = memoryInGigs;
    }

    public int getMemoryInGigs() {
        return memoryInGigs;
    }

    /**
     * Sets the job as a parallel job (i.e. can have a parallel execution in OGE)
     */
    public void setAsParallel() {
        parallel = true;
    }

    /**
     * States whether the job can have a parallel execution in OGE or not
     * @return true of the job can be parallelized, false otherwise
     */
    public boolean isParallel() {
        return parallel;
    }
}
