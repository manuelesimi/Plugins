package org.campagnelab.gobyweb.clustergateway.data;

import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.File;
import java.util.List;

/**
 *
 * A resource installation job.
 * @author Fabien Campagne
 *         Date: 3/20/13
 *         Time: 6:10 PM
 */
public class ResourceJob extends DataObject {
    ResourceConfig config;
    private File[] files;

    public ResourceJob(ResourceConfig config) {
        this.config = config;
    }

    public ResourceConfig getSourceConfig() {
        return config;
    }


    public File[] getFiles() {
        List<PluginFile> files= getSourceConfig().getFiles();
        File[] result=new File[files.size()];
        int i=0;
        for (PluginFile rFile: files) {
            result[i++]=rFile.getLocalFile();
        }
        return result;
    }
}
