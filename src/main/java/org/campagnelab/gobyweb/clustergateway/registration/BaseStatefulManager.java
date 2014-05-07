package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.io.Serializable;

/**
 * Base version of stateful FileSetManager.
 *
 * @author manuele
 */
public class BaseStatefulManager implements Serializable {

    protected  FileSetArea storageArea;

    protected PluginRegistry pluginRegistry;

    protected ConfigurationList configurationList;

    public BaseStatefulManager(String filesetAreaReference, String owner) throws IOException {
        this.storageArea = AreaFactory.createFileSetArea(filesetAreaReference, owner);
        this.pluginRegistry = PluginRegistry.getRegistry();
    }

    /**
     * Sets the plugin definitions to use for the manager's operation.
     * @param pluginRegistry
     * @throws Exception
     */
    public void setPluginDefinitions(PluginRegistry pluginRegistry) throws Exception {
        this.pluginRegistry = pluginRegistry;
        configurationList = PluginsToConfigurations.convertAsList(pluginRegistry.filterConfigs(FileSetConfig.class));
    }

}
