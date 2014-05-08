package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.preview.RegistrationPreviewDetails;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.plugins.PluginRegistry;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by mas2182 on 5/8/14.
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
}
