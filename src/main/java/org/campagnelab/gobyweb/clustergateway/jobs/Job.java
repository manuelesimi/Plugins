package org.campagnelab.gobyweb.clustergateway.jobs;

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;

import java.util.Date;

/**
 * Data object used for submission to the ClusterGateway.
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

}
