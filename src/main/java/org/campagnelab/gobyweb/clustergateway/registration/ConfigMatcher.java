package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import static org.campagnelab.gobyweb.clustergateway.registration.InputEntry.*;


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
           if (check(config,inputEntry))
               matchingConfigs.add(config);
       }
       return Collections.unmodifiableList(matchingConfigs);
   }

    /**
     * Checks if the entry can be assigned to an instance of the fileset configuration
     * @param config
     * @param inputEntry
     * @return
     */
   protected boolean check(FileSetConfig config, InputEntry inputEntry) {
        for (FileSetConfig.ComponentSelector selector : config.getFileSelectors()) {
            if (selector.getPattern().equalsIgnoreCase(inputEntry.getPattern())) {
                inputEntry.assignConfigEntry(selector.getId(), ENTRY_TYPE.FILE);
                return true;
            }
        }
        for (FileSetConfig.ComponentSelector selector : config.getDirSelectors()) {
            if (selector.getPattern().equalsIgnoreCase(inputEntry.getPattern())){
                inputEntry.assignConfigEntry(selector.getId(),ENTRY_TYPE.DIR);
                return true;
            }
        }
        return false;
    }
}
