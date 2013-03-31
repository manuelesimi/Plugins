package org.campagnelab.gobyweb.clustergateway.registration;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileWriter;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;

import java.io.IOException;
import java.io.File;
import java.util.*;

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
     * Registers one or more fileset instances according to the input entries
     * @param entries the list of entries for the fileset(s). They must be in one of the following forms:
     *   1) FILESET_CONFIG_ID:pattern
     *   2) FILESET_CONFIG_ID:filename
     *   3) pattern (e.g. *.compact_reads, **, etc)
     *   4) filename
     *
     * @throws IOException if the registration fails or any of the entries is not valid
     */
    protected void register(String[] entries) throws IOException {
       // FileSetConfig config = registry.findByTypedId(id, FileSetConfig.class);

        List<InputEntry> inputEntries = this.parseInputEntries(entries);
        FileSetInstanceBuilder builder = new FileSetInstanceBuilder(registry);
        List<FileSet> instancesToRegister = builder.buildList(inputEntries);
        if (builder.hasError()) {
            for (String error : builder.getErrorMessages())
                logger.error(error);
            return;
        }
        //push the files and metadata
        for (FileSet fileSet : instancesToRegister) {
            Map<String, InputEntryFile> files = builder.getMatchingFiles(fileSet);
            //prepare metadata
            MetadataFileWriter metadataFileWriter = new MetadataFileWriter(
                    fileSet.getId(),fileSet.getTag(),fileSet.getOwnerId());
            storageArea.createTag(fileSet.getTag());
            //push the files in the storage area
            try {
                for (Map.Entry<String,InputEntryFile> entry : files.entrySet()) {
                    logger.debug(String.format("Uploading file %s as entry %s in the storage area",entry.getValue().getAbsolutePath(), entry.getKey()));
                    storageArea.push(fileSet.getTag(),entry.getValue());
                    fileSet.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                    metadataFileWriter.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                    entry.getValue().setConsumed(true);
                }

            } catch (IOException e) {
                this.rollback(fileSet.getTag());
                throw e;
            } catch (IllegalArgumentException e) {
                this.rollback(fileSet.getTag());
                throw e;
            }

            //upload the fileset metadata for its correct consumption
            File serializedMetadata = null;
            try {
                serializedMetadata = metadataFileWriter.serialize();
                logger.debug(String.format("Uploading metadata file %s in the storage area",serializedMetadata.getAbsolutePath()));
                storageArea.pushMetadataFile(fileSet.getTag(),serializedMetadata);
            } catch (Exception e) {
                this.rollback(fileSet.getTag());
                throw new IOException(String.format("Failed to create or upload metadata for the fileset instance. Reason: %s",e.getStackTrace().toString()));
            } finally {
                if (serializedMetadata != null)
                    FileUtils.forceDelete(serializedMetadata);
            }
        }
        //check whether all the input entries have been consumed
        for (InputEntry inputEntry : inputEntries) {
            if (inputEntry.hasNexFile())
                logger.warn(String.format("Some files in the entry %s were not consumed because they didn't match any fileset configuration",inputEntry.getPattern()));
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
     * Parses the input entries.
     * @param entries the list of entries in the form of FILESET_ID:PATTERN or PATTERN
     * @return a map with [name -> file] for each entry
     * @throws IOException if any of the entries is not valid
     */
    private List<InputEntry> parseInputEntries(String[] entries) throws IOException {
        List<InputEntry> inputEntries = new ArrayList<InputEntry>();
        for (String entry : entries) {
            StringTokenizer tokenizer = new StringTokenizer(entry, ":");
            switch (tokenizer.countTokens()) {
                case 1: inputEntries.add(new InputEntry(tokenizer.nextToken().trim()));break;
                case 2: inputEntries.add(new InputEntry(tokenizer.nextToken().trim(), tokenizer.nextToken().trim()));break;
                default: throw new IOException(String.format("Invalid entry format: %s. Entries must be in the form FILESET_ID:PATTERN or PATTERN", entry));
            }
        }
        return inputEntries;
    }



}
