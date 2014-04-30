package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.filesets.preview.RegistrationPreviewDetails;
import org.campagnelab.gobyweb.filesets.registration.InputEntry;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A stateful version of the FileSetManager.
 *
 * @author manuele
 */
public class StatefulFileSetManager {

    private final FileSetArea storageArea;
    private PluginRegistry pluginRegistry;
    ConfigurationList configurationList;

    public StatefulFileSetManager(String filesetAreaReference, String owner) throws IOException {
        this.storageArea = AreaFactory.createFileSetArea(
                filesetAreaReference, owner);
        this.pluginRegistry = PluginRegistry.getRegistry();
    }

    public void setPluginDefinitions(PluginRegistry pluginRegistry) throws Exception {
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

    /**
     * Registers a single instance fileset instance using the given type.
     * @param fileSetID the type of fileset to register
     * @param paths list of pathnames matching the fileset entries
     * @param attributes
     * @param sharedWith
     * @param errors
     * @param tag
     * @return the list of tags of the newly registered filesets.
     * @throws Exception
     */
    public List<String> register(String fileSetID,
            String[] paths, final Map<String, String> attributes,
            final List<String> sharedWith, List<String> errors, String tag) throws Exception {
        String[] entries = new String[paths.length + 1];
        entries[0] = fileSetID + ":";
        System.arraycopy(paths,0,entries,1,paths.length);
        FileSetAPI fileset = FileSetAPI.getReadWriteAPI(storageArea, configurationList);
        List<InputEntry> inputEntries = FileSetManager.parseInputEntries(entries);
        return fileset.register(inputEntries,attributes,sharedWith,errors,tag);
    }
}