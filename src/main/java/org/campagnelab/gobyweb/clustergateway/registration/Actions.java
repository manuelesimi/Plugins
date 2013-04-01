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
     * @param sourceDir the directory where files are located. if not specified, the current folder is used.
     * @return the tags of the registered instances
     * @throws IOException if the registration fails or any of the entries is not valid
     */
    protected List<String> register(final String[] entries, String ... sourceDir) throws IOException {
        List<String> tags = new ArrayList<String>();
        List<InputEntry> inputEntries = this.parseInputEntries(entries,
                (sourceDir!=null&&sourceDir.length>0)?sourceDir[0]:System.getProperty("user.dir"));
        FileSetInstanceBuilder builder = new FileSetInstanceBuilder(registry);
        List<FileSet> instancesToRegister = builder.buildList(inputEntries);
        if (builder.hasError()) {
            for (String error : builder.getErrorMessages())
                logger.error(error);
            //throw new IOException();
        }
        //push the files and metadata
        for (FileSet fileSet : instancesToRegister) {
            logger.info(String.format("Registering an instance of FileSet %s with tag %s", fileSet.getId(),fileSet.getTag()));
            MetadataFileWriter metadataFileWriter = null;
            try {
                Map<String, InputEntryFile> files = builder.getAssignedFiles(fileSet);
                //prepare metadata
                fileSet.setOwner(storageArea.getOwner());
                metadataFileWriter = new MetadataFileWriter(
                        fileSet.getId(),fileSet.getTag(),fileSet.getOwnerId());
                storageArea.createTag(fileSet.getTag());
                //push the files in the storage area
                for (Map.Entry<String,InputEntryFile> entry : files.entrySet()) {
                    logger.trace(String.format("Uploading file %s as entry %s in the storage area", entry.getValue().getAbsolutePath(), entry.getKey()));
                    storageArea.push(fileSet.getTag(),entry.getValue());
                    fileSet.addEntry(entry.getKey(),entry.getValue());
                    metadataFileWriter.addEntry(entry.getKey(),entry.getValue().getName(), FileUtils.sizeOf(entry.getValue()));
                }
            } catch (IOException e) {
                this.rollback(fileSet.getTag());
                throw e;
            } catch (IllegalArgumentException e) {
                this.rollback(fileSet.getTag());
                throw e;
            } catch (FileSetInstanceBuilder.IncompleteInstanceException e) {
                this.rollback(fileSet.getTag());
                throw new IOException("Incomplete FileSet instance");
            }

            //upload the fileset metadata for its correct consumption
            File serializedMetadata = null;
            try {
                if (metadataFileWriter != null) {
                    serializedMetadata = metadataFileWriter.serialize();
                    logger.trace(String.format("Uploading metadata file %s in the storage area", serializedMetadata.getAbsolutePath()));
                    storageArea.pushMetadataFile(fileSet.getTag(),serializedMetadata);
                } else {
                    logger.warn("The FileSet instance does not have any metadata associated");
                }
            } catch (Exception e) {
                this.rollback(fileSet.getTag());
                throw new IOException(String.format("Failed to create or upload metadata for the fileset instance. Reason: %s",e.getStackTrace().toString()));
            } finally {
                if (serializedMetadata != null)
                    FileUtils.forceDelete(serializedMetadata);
            }
            logger.info("FileSet successfully registered");
            //add the instance tag to the list to return
            tags.add(fileSet.getTag());
        }
        //check whether all the input entries have been consumed
        for (InputEntry inputEntry : inputEntries) {
            if (inputEntry.hasNextFile())
                logger.warn(String.format("Some files in the entry %s were not consumed because they didn't match any fileset configuration",inputEntry.getPattern()));
        }

        return Collections.unmodifiableList(tags);
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
    private List<InputEntry> parseInputEntries(final String[] entries, String dir) throws IOException {
        List<InputEntry> inputEntries = new ArrayList<InputEntry>();
        for (String entry : entries) {
            StringTokenizer tokenizer = new StringTokenizer(entry, ":");
            switch (tokenizer.countTokens()) {
                case 1: inputEntries.add(new InputEntry(dir, tokenizer.nextToken().trim()));break;
                case 2: inputEntries.add(new InputEntry(dir, tokenizer.nextToken().trim(), tokenizer.nextToken().trim()));break;
                default: throw new IOException(String.format("Invalid entry format: %s. Entries must be in the form FILESET_ID:PATTERN or PATTERN", entry));
            }
        }
        return Collections.unmodifiableList(inputEntries);
    }



}
