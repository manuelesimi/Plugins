package org.campagnelab.gobyweb.clustergateway.registration;

import com.esotericsoftware.wildcard.Paths;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import static org.campagnelab.gobyweb.clustergateway.registration.InputEntry.*;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds a fileset instance configuration that matches the input entry.
 *
 *  @author manuele
 */
class ConfigMatcher {

   private List<FileSetConfig> configs;

    protected ConfigMatcher(PluginRegistry registry){
       configs = registry.filterConfigs(FileSetConfig.class);
    }

    /**
     * Looks for fileset configurations that match the entry
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
        for (FileSetConfig.ComponentSelector selector : config.getFileSelectors()) {
            if (assignSelector(inputEntry, selector, ENTRY_TYPE.FILE))
                return true;
        }
        for (FileSetConfig.ComponentSelector selector : config.getDirSelectors()) {
            if (assignSelector(inputEntry, selector, ENTRY_TYPE.DIR))
                return true;
        }
        return false;
    }

    /**
     * Tries to assign the input entry to the given selector
     * @param inputEntry
     * @param selector
     * @param type
     * @return true if the entry was assigned, false otherwise
     */
    private boolean assignSelector(InputEntry inputEntry,
                       FileSetConfig.ComponentSelector selector,  ENTRY_TYPE type) {
        if (inputEntry.getPattern() == null) {
            //a filename has been specified
            Paths paths = new Paths();  //see http://code.google.com/p/wildcard/
            paths.glob(inputEntry.getFile().getParentFile().getAbsolutePath(), selector.getPattern());
            for (File matchedFile: paths.getFiles()) {
               if (matchedFile.getAbsolutePath().equalsIgnoreCase(inputEntry.getFile().getAbsolutePath())) {
                   //same file, the input entry file matched the pattern
                   inputEntry.assignConfigEntry(selector.getId(),type);
                   return true;
               }
            }


        } else {
            if (selector.getPattern().equalsIgnoreCase(inputEntry.getPattern())){
                inputEntry.assignConfigEntry(selector.getId(),type);
                return true;
            }
        }
        return false;
    }
}