package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.preview.RegistrationPreviewDetails;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.plugins.PluginRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Interface for stateful managers.
 *
 * @author manuele
 */
public interface StatefulFileSetManager {

    /**
     * Sets the plugin definitions to use for the manager's operation.
     * @param pluginRegistry
     * @throws Exception
     */
    public void setPluginDefinitions(PluginRegistry pluginRegistry);

    /**
     * Previews the registration of the given paths.
     * @param paths paths to preview
     * @param fileSetID an optional fileset ID to use for the preview.
     * @return
     * @throws Exception
     */
    public RegistrationPreviewDetails previewRegistration(String[] paths, String ... fileSetID) throws Exception;

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
    public List<String> register(String fileSetID, String[] paths, final Map<String, String> attributes,
                                 final List<String> sharedWith, List<String> errors, String tag) throws Exception;

    /**
     * Fetches the instance metadata.
     * @param tag
     * @param errors
     * @return
     * @throws IOException
     */
    public MetadataFileReader fetchMetadata(String tag, List<String> errors) throws IOException;

    /**
     * Fetches an entry from the instance.
     * @param entryName
     * @param tag
     * @param paths the localized files
     * @param errors
     * @return
     * @throws IOException
     */
    public boolean fetchEntry(String entryName, String tag, List<String> paths, List<String> errors) throws IOException;

    /**
     * Fetches an entry from the instance as stream of bytes.
     * @param entryName
     * @param tag
     * @param data
     * @param errors
     * @return
     * @throws IOException
     */
    public boolean fetchStreamedEntry(String entryName, String tag, List<ByteBuffer> data, List<String> errors) throws IOException;

    /**
     * Downloads listed entries from the instance.
     * @param tag
     * @param entries
     * @param errors
     * @return a map entry name -> downloaded files belonging to the entry
     * @throws IOException
     */
    public Map<String, List<String>> download(String tag, List<String> entries, List<String> errors) throws IOException;

    /**
     * Shares an instance with the listed users.
     * @param tag
     * @param sharedWith
     * @param errors
     * @return true, if all attributes were successfully edited, false otherwise
     */
    public boolean shareWith(String tag, List<String> sharedWith, List<String> errors) throws Exception;

}
