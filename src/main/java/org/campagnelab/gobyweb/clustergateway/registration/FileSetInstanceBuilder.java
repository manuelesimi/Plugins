package org.campagnelab.gobyweb.clustergateway.registration;

import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.util.*;

/**
 * Builder for fileset instances
 *
 * @author manuele
 */
class FileSetInstanceBuilder {

    private static Logger logger = Logger.getLogger(FileSetInstanceBuilder.class);

    private final List<String> errorMessages = new ArrayList<String>();

    private final PluginRegistry registry;

    protected FileSetInstanceBuilder(PluginRegistry registry) {
        this.registry = registry;
    }

    /**
     * Starting from the input entries, creates a list of fileset instances to be registered.
     * @param inputEntries
     * @return
     */
    protected List<FileSet> buildList(List<InputEntry> inputEntries) {
        this.errorMessages.clear();
        List<FileSet> instances = new ArrayList<FileSet>();
        for (InputEntry inputEntry : inputEntries) {
            //get a matching configuration
            FileSetConfig config = null;
            try {
                config = lookForMatchingConfig(inputEntry);
            } catch (ConfigNotFoundException e) {
                inputEntry.markAsConsumed();
                continue;
            } catch (TooManyConfigsException e) {
                inputEntry.markAsConsumed();
                continue;
            }
            if (inputEntry.getFileSetEntryType() == InputEntry.ENTRY_TYPE.FILE) {
                //create an instance for each entry file
                while (inputEntry.hasNexFile()) {
                    FileSet instance = new FileSet(config);
                    InputEntryFile file = inputEntry.nextFile();
                    instance.setId(config.getId());
                    instance.setTag(ICBStringUtils.generateRandomString(7));
                    instance.setBasename(file.getName());
                    //assign entry file
                    try {
                        instance.addEntry(inputEntry.getAssignedEntryName(), file);
                        if (!instance.isComplete()) {
                            //TODO: complete the instance

                        }

                    } catch (IOException e) {
                        errorMessages.add("Failed to create fileset instance for " + inputEntry.getPattern());
                        inputEntry.markAsConsumed();
                    }
                    file.setConsumed(true);
                    instances.add(instance);
                }
            } else {
                FileSet instance = new FileSet(config);
                instance.setId(config.getId());
                instance.setTag(ICBStringUtils.generateRandomString(7));
                //TODO: assign all the entry files to the config

                if (!instance.isComplete()) {

                }
                instances.add(instance);
            }
        }
        return Collections.unmodifiableList(instances);
    }
    /**
     * Looks for fileset configurations matching the input entry.
     * The matching configurations could be partially satisfied by the entry files.
     *
     * @param inputEntry
     * @throws ConfigNotFoundException if no configuration was found
     * @throws TooManyConfigsException if multiple configurations have been found
     */
    private FileSetConfig lookForMatchingConfig(InputEntry inputEntry)
            throws ConfigNotFoundException, TooManyConfigsException {
        if (inputEntry.isBoundToFileSet()) { //the configuration has been specified by the user
            FileSetConfig config = registry.findByTypedId(inputEntry.getFileSetId(), FileSetConfig.class);
            if (config == null) {
                errorMessages.add("Unable to find fileset configuration: " + inputEntry.getFileSetId());
                throw new ConfigNotFoundException();
            } else {
                if (new ConfigMatcher(registry).assign(config, inputEntry)) {
                    return config;
                } else {
                    errorMessages.add("Failed to assign the input entry to the FileSet configuration: " + inputEntry.getFileSetId());
                    throw new ConfigNotFoundException();
                }
            }

        } else { //try to match the entry with the appropriate configuration
            List<FileSetConfig> configs = new ConfigMatcher(registry).match(inputEntry);
            if (configs.size() == 0) {
                errorMessages.add(String.format("Unable to find a fileset configuration to which the entry %s could be matched", inputEntry.getPattern()));
                throw new ConfigNotFoundException();
            }
            if (configs.size() == 1) {
                return configs.get(0);
            } else {
                //TODO: we could be smarter here and process all the configs for checking the best match with the next entries?
                errorMessages.add(String.format("Too many matching fileset configurations. The input entry %s matched more than one fileset configuration. Impossible to manage it.", inputEntry.getPattern()));
                errorMessages.add("Compatible configurations:");
                for (FileSetConfig fsc : configs)
                    errorMessages.add("\t" + fsc.getId());
                errorMessages.add("The registration cannot be completed. Resubmit the registration by specifying the fileset configuration id");
                throw new TooManyConfigsException();
            }
        }
    }

    /**
     * Checks if the builder encountered any error on the operations.
     * For further details on errors, check {@link FileSetInstanceBuilder#getErrorMessages()}
     * @return true if there is any error, false otherwise
     */
    protected boolean hasError() {
        return this.errorMessages.size()>0? true : false;
    }

    /**
     * Gets the error messages from the latest build operation
     * @return
     */
    protected List<String> getErrorMessages() {
        return Collections.unmodifiableList(this.errorMessages);
    }

    /**
     * Gets all the input files that have been assigned to the fileset.
     * @param fileSet
     * @return
     */
    protected Map<String,InputEntryFile> getAssignedFiles(FileSet fileSet) throws IncompleteInstanceException {
        Map<String,InputEntryFile> files = new HashMap<String, InputEntryFile>();
        for (String entry: fileSet.getAllEntryNames()) {
            try {
                files.put(entry, (InputEntryFile) fileSet.getEntryFile(entry));
            } catch (IOException ioe) {
               throw new IncompleteInstanceException(String.format("Entry %s is not complete.", entry));
            }
        }
        return files;
    }

    /**
     * The FileSet instance is not complete. There exist mandatory entries with no file(s) assigned.
     */
    protected static class IncompleteInstanceException extends Exception {

        public IncompleteInstanceException() {
            super();
        }

        public IncompleteInstanceException(String message) {
            super(message);
        }
    }

    /**
     * No fileset configuration matching a pattern has been found
     */
    protected static class ConfigNotFoundException extends Exception {

        public ConfigNotFoundException() {
            super();
        }
    }

    /**
     * No fileset configuration matching a pattern has been found
     */
    protected static class TooManyConfigsException extends Exception {

        public TooManyConfigsException() {
            super();
        }
    }
}
