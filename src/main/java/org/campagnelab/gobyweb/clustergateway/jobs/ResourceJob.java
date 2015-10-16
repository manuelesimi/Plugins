package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.clustergateway.submission.SubmissionRequest;
import org.campagnelab.gobyweb.plugins.xml.Config;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.List;

/**
 *
 * A resource installation job.
 *
 * @author Fabien Campagne
 *         Date: 3/20/13
 *         Time: 6:10 PM
 */
public class ResourceJob extends Job {

    private List<ResourceConfig> configs;

    /** attributes values specified by the user on the command line */
    private SubmissionRequest.ArtifactInfoMap attributes;

    public ResourceJob(List<ResourceConfig> configs) {
        this.configs = configs;
        for (ResourceConfig config : configs) {
            for (PluginFile file : config.getFiles())
                this.addFile(file.getLocalFile());
        }
    }


    public List<ResourceConfig> getSourceConfigs() {
        return configs;
    }

    /**
     * Converts the source ResourceConfigs as base Configs
     * @return
     */
    public List<Config> getBaseSourceConfigs() {
        return (List<Config>)(List<?>) configs;
    }


    public void setAttributes(SubmissionRequest.ArtifactInfoMap attributes) {
        this.attributes = attributes;
    }

    public SubmissionRequest.ArtifactInfoMap getAttributes() {
        return attributes;
    }
}
