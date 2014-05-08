package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.filesets.preview.RegistrationPreviewDetails;
import org.campagnelab.gobyweb.filesets.registration.InputEntry;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Base version of stateful FileSetManager.
 *
 * @author manuele
 */
public abstract class BaseStatefulManager implements Serializable,StatefulFileSetManager {

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
    public void setPluginDefinitions(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        configurationList = PluginsToConfigurations.convertAsList(pluginRegistry.filterConfigs(FileSetConfig.class));
    }
    /**
     * Previews the registration of the given paths.
     * @param paths paths to preview
     * @param fileSetID an optional fileset ID to use for the preview.
     * @return
     * @throws Exception
     */
    public RegistrationPreviewDetails previewRegistration(
            String[] paths, String ... fileSetID) throws Exception {
        //convert plugins configuration to configurations that can be consumed by FileSetAPI
        FileSetAPI fileset = FileSetAPI.getReadWriteAPI(storageArea, configurationList);
        List<InputEntry> inputEntries;
        if (fileSetID != null && fileSetID.length > 0) {
            String[] entries = new String[paths.length + 1];
            entries[0] = fileSetID[0] + ":";
            System.arraycopy(paths,0,entries,1,paths.length);
            inputEntries = FileSetManager.parseInputEntries(entries);
        }  else {
            inputEntries = FileSetManager.parseInputEntries(paths);
        }
        return fileset.registerPreview(inputEntries,fileSetID);
    }

}
