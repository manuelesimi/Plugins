package org.campagnelab.gobyweb.clustergateway.jobs;

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;

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

    private Map<String, Object> replacements = new HashMap<String, Object>();

    private int memoryInGigs;

    private int memoryOverheadInGigs;

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
     * Adds the additional replacements to the Job.
     * @param additionalReplacements
     */
    protected void addReplacements(Map<String, Object> additionalReplacements) {
        this.replacements.putAll(additionalReplacements);
    }

    public Map<String, Object> getReplacementsMap() {
        return replacements;
    }

    public void setMemoryInGigs(int memoryInGigs) {
        this.memoryInGigs = memoryInGigs;
    }

    public int getMemoryInGigs() {
        return memoryInGigs;
    }

    public void setMemoryOverheadInGigs(int memoryOverheadInGigs) {
        this.memoryOverheadInGigs = memoryOverheadInGigs;
    }

    public int getMemoryOverheadInGigs() {
        return memoryOverheadInGigs;
    }

}
