package org.campagnelab.gobyweb.clustergateway.registration;

import com.esotericsoftware.wildcard.Paths;
import org.apache.commons.io.FilenameUtils;
import org.campagnelab.gobyweb.clustergateway.data.FileSet;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Matches input entries with the available FileSet configurations.
 *
 *  @author manuele
 */
class ConfigMatcher {

   private List<FileSetConfig> configs;

    protected ConfigMatcher(PluginRegistry registry){
       configs = registry.filterConfigs(FileSetConfig.class);
    }

    /**
     * Looks for fileset configurations that match the entry.
     * @param inputEntry
     * @return
     */
   protected List<FileSetConfig> match(InputEntry inputEntry) {
       List<FileSetConfig> matchingConfigs = new ArrayList<FileSetConfig>();
       for (FileSetConfig config : configs) {
           if (assign(config, inputEntry))
               matchingConfigs.add(config);
       }
       return Collections.unmodifiableList(matchingConfigs);
   }

    /**
     * Tries to bind the entry to the given fileset configuration.
     * Once bound, files of this entry will be assigned only to
     * instances of this configuration.
     * @param config
     * @param inputEntry
     * @return true if the entry has been assigned
     */
   protected boolean assign(FileSetConfig config, InputEntry inputEntry) {
        //we need two loops because the selector types might not have been initialized (could be improved)
        for (FileSetConfig.ComponentSelector selector : config.getFileSelectors()) {
            if (assignSelector(inputEntry, selector))
                return true;
        }
        for (FileSetConfig.ComponentSelector selector : config.getDirSelectors()) {
            if (assignSelector(inputEntry, selector))
                return true;
        }
        return false;
    }

    /**
     * Tries to assign the input entry to the given selector.
     * @param inputEntry
     * @param selector
     * @return true if the entry was assigned, false otherwise
     */
    private boolean assignSelector(InputEntry inputEntry,
                       FileSetConfig.ComponentSelector selector) {

        if (this.matchSelector(inputEntry,selector)) {
            inputEntry.assignConfigEntry(selector.getId(),selector.getType());
            return true;
        }
        return false;
    }

    /**
     * Matches the entry with the selector.
     * @param inputEntry
     * @param selector
     * @return true if they match, false otherwise
     */
    private boolean matchSelector(InputEntry inputEntry, FileSetConfig.ComponentSelector selector) {
        if (inputEntry.getPattern() == null) {
            //a filename has been specified
            Paths paths = new Paths();  //see http://code.google.com/p/wildcard/
            paths.glob(inputEntry.getFile().getParentFile().getAbsolutePath(), selector.getPattern());
            for (File matchedFile: paths.getFiles()) {
                if (matchedFile.getAbsolutePath().equalsIgnoreCase(inputEntry.getFile().getAbsolutePath())) {
                    //same file, the input entry file matched the pattern
                    return true;
                }
            }
        } else {
            if (selector.getPattern().equalsIgnoreCase(inputEntry.getPattern())){
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to complete the fileset instance with the remaining entries.
     * @param instance the instance to complete
     * @param sourceEntry the entry used to create the instance
     * @param inputEntries the list of the other entries available
     * @return  true if the instance was successfully completed, false otherwise
     */
    public boolean completeInstance(FileSet instance, InputEntry sourceEntry, final List<InputEntry> inputEntries) {
        String basename = instance.getBasename();
        for (String configEntryName : instance.getMissingEntries(true)) {
            FileSetConfig.ComponentSelector selector = instance.getSourceConfig().getSelector(configEntryName);
            if (selector == null) //it should never occur because the name was taken from the corresponding instance
                continue;
                entry: for (InputEntry inputEntry : inputEntries) {
               //check if the entry files can be assigned to the selector
                if (this.matchSelector(inputEntry,selector)) {
                    for (InputEntryFile file : inputEntry.getFiles()) {
                        if (basename.equalsIgnoreCase(FilenameUtils.removeExtension(file.getName()))) {
                            //found it!!
                            inputEntry.assignConfigEntry(selector.getId(),selector.getType());
                            instance.addEntry(selector.getId(),file);
                            file.setConsumed(true);
                            break entry;
                        }
                    }
                }
            }
        }
        return instance.isComplete();
    }
}