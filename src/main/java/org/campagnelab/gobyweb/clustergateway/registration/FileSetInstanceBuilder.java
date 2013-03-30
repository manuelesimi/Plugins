package org.campagnelab.gobyweb.clustergateway.registration;

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builder for fileset instances
 *
 * @author manuele
 */
class FileSetInstanceBuilder {

    private static Logger logger = Logger.getLogger(FileSetInstanceBuilder.class);

    private FileSet instance;

    private FileSetConfig config;

    private final List<String> errorMessages = new ArrayList<String>();

    private List<InputEntry> otherEntries;

    private final ConfigMatcher matcher;

    protected FileSetInstanceBuilder(PluginRegistry registry) {
         matcher = new ConfigMatcher(registry);
    }

    /**
     * Starting from the input entries, creates a list of fileset instances to be registered.
     * @param inputEntries
     * @return
     */
    protected List<FileSet> buildList(List<InputEntry> inputEntries) {
        for (InputEntry inputEntry : inputEntries) {

        }
    }
    /**
     * Looks for fileset configurations matching the input entry.
     * The matching configurations could be partially satisfied by the entry files.
     *
     * @param inputEntry
     */
    private void lookForMatchingConfig(InputEntry inputEntry) {
        if (inputEntry.isBoundToFileSet()) {
            config = registry.findByTypedId(inputEntry.getFileSetId(), FileSetConfig.class);
            instance = new FileSet(config);
        } else {
            List<FileSetConfig> configs = new ConfigMatcher(registry).match(inputEntry);
            if (configs.size() == 0) {
                errorMessages.add(String.format("Unable to find a fileset configuration to which the entry %s could be matched", inputEntry.getPattern()));
            }
            if (configs.size() == 1) {
                config = configs.get(0);
            } else {
                //TODO: could be smarter here and process all the configs checking the best match with the next entries?
                errorMessages.add(String.format("Too many matching fileset configurations. The input entry %s matched more than one fileset configuration. Impossible to manage it.", inputEntry.getPattern()));
                errorMessages.add("Compatible configurations:");
                for (FileSetConfig fsc : configs)
                    errorMessages.add("\t" + fsc.getId());
                errorMessages.add("The registration cannot be completed. Resubmit the registration by specifying the fileset configuration id");
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
     *
     * @return
     */
    protected List<String> getErrorMessages() {
        return Collections.unmodifiableList(this.errorMessages);
    }

    /**
     * Tries to build the fileset instance with the current available information
     * @throws InstanceNotCompleteException
     */
    protected FileSet build() throws InstanceNotCompleteException {
        instance = new FileSet(config);
        this.build(instance, initialEntry );

        return instance;
    }


    private void build(FileSet fileset, InputEntry inputEntry) throws InstanceNotCompleteException {
        for (FileSetConfig.ComponentSelector selector : config.getFileSelectors()) {
            String pattern = selector.getPattern();
            if (pattern.equalsIgnoreCase(initialEntry.getPattern())) {
                //TODO great, we can assign the entry files to this selector
                initialEntry.getFiles();
                fileset.setId(config.getId());
            } else if (selector.getMandatory()) {
                //try to complete with the other entries, if any
                if (otherEntries.size()>0) {
                    for (InputEntry otherEntry : otherEntries) {

                    }
                } else {
                    throw new InstanceNotCompleteException();
                }

            }
        }
    }


    protected FileSet tryToComplete(List<InputEntry> inputEntries) throws InstanceNotCompleteException  {
        this.otherEntries = inputEntries;
        return this.build();
    }

    protected Map<String,InputEntryFile> getMatchingFiles(FileSet fileSet) {

    }


    protected static class InstanceNotCompleteException extends Exception {

        public InstanceNotCompleteException() {
            super();
        }
    }

}
