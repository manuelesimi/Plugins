package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.filesets.preview.RegistrationPreviewDetails;
import org.campagnelab.gobyweb.filesets.registration.InputEntry;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base version of stateful FileSetManager.
 *
 * @author manuele
 */
public abstract class BaseStatefulManager implements Serializable,StatefulFileSetManager {

    protected  FileSetArea storageArea;

    protected PluginRegistry pluginRegistry;

    protected ConfigurationList configurationList;

    protected BaseStatefulManager() {

    }
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
        FileSetAPI fileset = FileSetAPI.getReadWriteAPI(AreaFactory.createDummyFileSetArea(), configurationList);
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



    /**
     * Downloads listed entries from the instance.
     *
     * @param tag
     * @param entries
     * @param errors
     * @return a map entry name -> downloaded files belonging to the entry
     * @throws java.io.IOException
     */
    @Override
    public Map<String, List<String>> download(String tag, List<String> entries, List<String> errors) throws IOException {
        FileSetAPI fileset = FileSetAPI.getReadOnlyAPI(this.storageArea);
        Map<String, List<String>> fetchedEntries = new HashMap<String, List<String>>(entries.size());
        for (String entryName : entries) {
            List<String> paths = new ArrayList<String>();
            if (fileset.fetchEntry(entryName,tag,paths,errors)) {
              fetchedEntries.put(entryName,paths);
            }
        }
        return fetchedEntries;
    }
}
