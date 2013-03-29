package org.campagnelab.gobyweb.clustergateway.registration;

import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileWriter;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

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
     * Registers a fileset instance with its entries.
     * @param entries the list of entries for the fileset. They must be in one of the following forms:
     *   FILESET_CONFIG_ID:pattern
     *   FILESET_CONFIG_ID:filename
     *   pattern (e.g. *.compact_reads, **, etc)
     *   filename
     *
     * @throws IOException if the registration fails or any of the entries is not valid
     */
    protected void register(String[] entries) throws IOException {
       // FileSetConfig config = registry.findByTypedId(id, FileSetConfig.class);

        List<InputEntry> inputEntries = this.parseInputEntries(entries);

        for (InputEntry inputEntry : inputEntries) {
            String tag = ICBStringUtils.generateRandomString();
            if (inputEntry.isConsumed()) //nothing to process for this entry
                continue;
            FileSetConfig config = getMatchingFileSetConfig(inputEntry) ;
            Map<String, InputEntryFile> files = getMatchingFiles(config, inputEntry);
            if (! config.isComplete(files)) {
                // TODO search for missing files in the other input entries (consider only files not used, yet)

                files = completeConfig(config, inputEntry, inputEntries, files);
                if (! config.isComplete(files)) {
                    // exception
                }

            }

            //create config instance
            FileSet instance = new FileSet(config);
            instance.setBasename(storageArea.createTag(tag));
            instance.setTag(tag);
            instance.setId(config.getId());
            //prepare metadata
            MetadataFileWriter metadataFileWriter = new MetadataFileWriter(
                    instance.getId(),instance.getTag(),instance.getOwnerId());

            //push the files in the storage area
            try {
                for (Map.Entry<String,InputEntryFile> entry : files.entrySet()) {
                    logger.debug(String.format("Uploading file %s as entry %s in the storage area",entry.getValue().getAbsolutePath(), entry.getKey()));
                    storageArea.push(tag,entry.getValue());
                    instance.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                    metadataFileWriter.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                    entry.getValue().setConsumed(true);
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

        }  //end for on input entries

        //check whether all the input entries have been consumed
        for (InputEntry inputEntry : inputEntries) {
            if (!inputEntry.isConsumed())
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


    private FileSetConfig getMatchingFileSetConfig(InputEntry inputEntry) {
        FileSetConfig config = null ;
        if (inputEntry.isBoundToFileSet()) {
            config = registry.findByTypedId(inputEntry.getFileSetId(), FileSetConfig.class);
        } else {
            List<FileSetConfig> configs = new FileSetConfigMatcher(inputEntry).getConfigurations();
            if (configs.size() == 0) {
                throw new IllegalArgumentException("Unable to find any fileset configuration to which the entry belong to");
            } if (configs.size() == 1) {
                config = configs.get(0);
            } else {
                logger.warn(String.format("The input entry %s matched more than one fileset configuration. Impossible to manage it.",inputEntry.getPattern()));
                logger.warn("Compatible configurations:");
                for (FileSetConfig fsc : configs)
                    logger.warn("\t" + fsc.getId());
                logger.warn("Resubmit the registration by specifying the fileset configuration id");
                throw new IllegalArgumentException("Too many matching fileset configurations");

            }
        }
        return config;
    }
}
