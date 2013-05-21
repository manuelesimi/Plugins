package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.File;
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

    private ResourceConfig config;

    public ResourceJob(ResourceConfig config) {
        this.config = config;
        for (PluginFile file : config.getFiles())
            this.addFile(file.getLocalFile());

    }

    public ResourceConfig getSourceConfig() {
        return config;
    }
}
