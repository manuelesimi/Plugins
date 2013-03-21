package org.campagnelab.gobyweb.plugins.xml;

import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;

import java.util.List;

/**
 * @author Fabien Campagne
 *         Date: 3/21/13
 *         Time: 10:46 AM
 */
public interface PluginFileProvider {
    /**
     * Return the set of files held by this plugin.
     * @return a set of files defined for a plugin config.
     */
    public List<PluginFile> getFiles();
}
