package org.campagnelab.gobyweb.clustergateway.registration;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileWriter;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Execute actions requested through the command line for FileSetRegistration.
 *
 * @author manuele
 */
final class Actions {

    private FileSetArea storageArea;

    private PluginRegistry registry;

    private static Logger logger = Logger.getLogger(Actions.class);

    private Actions() {}

    /**
     *
     * @param storageArea
     * @param registry
     */
    protected Actions(FileSetArea storageArea, PluginRegistry registry) {
        this.registry = registry;
        this.storageArea = storageArea;
    }

    /**
     * Registers a fileset instance with its entries.
     * @param id the id of the fileset as reported in its plugin configuration
     * @param entries the list of entries for the fileset. They must be in  the form of ENTRY_NAME:ABSOLUTE PATH
     * @param tag the tag to assign at the instance
     * @throws IOException if the registration fails or any of the entries is not valid
     */
    protected void register(String id, String[] entries, String tag) throws IOException {
        FileSetConfig config = registry.findByTypedId(id, FileSetConfig.class);

        if (config == null)
            throw new IllegalArgumentException(String.format("A fileset configuration with id=%s does not exist",id));

        FileSet instance = new FileSet(config);
        if (storageArea.exists(tag))
            throw new IllegalArgumentException(String.format("A fileset instance with tag=%s already exists",tag));
        else
            instance.setBasename(storageArea.createTag(tag));
        instance.setId(id);
        instance.setTag(tag);
        MetadataFileWriter metadataFileWriter = new MetadataFileWriter(
                instance.getId(),instance.getTag(),instance.getOwnerId());
        try {
            Map<String, File> inputEntries = this.parseInputEntries(entries);
            for (Map.Entry<String,File> entry : inputEntries.entrySet()) {
                if (instance.validateEntry(entry.getKey(), entry.getValue())) {
                    logger.debug(String.format("Uploading file %s as entry %s in the storage area",entry.getValue().getAbsolutePath(), entry.getKey()));
                    storageArea.push(tag,entry.getValue());
                    instance.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                    metadataFileWriter.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                } else {
                    throw new IllegalArgumentException(String.format("Entry=%s does not match the fileset configuration", entry.getKey()));
                }
            }
        } catch (IOException e) {
            this.rollback(tag);
            throw e;
        } catch (IllegalArgumentException e) {
            this.rollback(tag);
            throw e;
        }
        //upload the fileset metadata for its correct consumption
        File serializedMetadata = null;
        try {
            serializedMetadata = metadataFileWriter.serialize();
            logger.debug(String.format("Uploading metadata file %s in the storage area",serializedMetadata.getAbsolutePath()));
            storageArea.pushMetadataFile(tag,serializedMetadata);
        } catch (Exception e) {
            this.rollback(tag);
            throw new IOException(String.format("Failed to create or upload metadata for the fileset instance. Reason: %s",e.getStackTrace().toString()));
        } finally {
            if (serializedMetadata != null)
                FileUtils.forceDelete(serializedMetadata);
        }
    }

    private void rollback(String tag) throws IOException {
        this.unregister(tag);
    }
    /**
     * Unregisters a fileset instance.
     * @param tag the tag to assign at the instance
     * @throws IOException
     */
    public void unregister(String tag) throws IOException {
        if (storageArea.exists(tag))
            storageArea.deleteTag(tag);
        else
            throw new IllegalArgumentException(String.format("A fileset instance with tag=%s does not exist",tag));
    }

    /**
     * Parses the input entries and return a map with entry name -> entry file.
     * @param entries the list of entries in the form of ENTRY_NAME:ABSOLUTE PATH
     * @return a map with [name -> file] for each entry
     * @throws IOException if any of the entries is not valid
     */
    private Map<String, File> parseInputEntries(String[] entries) throws IOException {
        Map<String, File> inputEntries = new HashMap<String, File>();
        for (String entry : entries) {
            StringTokenizer tokenizer = new StringTokenizer(entry, ":");
            if (tokenizer.countTokens() != 2)
                throw new IOException(String.format("Invalid entry format: %s. Entries must be in the form ENTRY:FILE", entry));
            String name = tokenizer.nextToken().trim();
            File file = new File (tokenizer.nextToken().trim());
            if(!file.exists())
                throw new IOException(String.format("File %s for entry %s does not exist",name, file.getAbsolutePath()));
            inputEntries.put(name,file);
        }
        return inputEntries;
    }

}
