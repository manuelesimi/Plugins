package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

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
           boolean matched = false;
           for (FileSetConfig.ComponentSelector selector : config.getFileSelectors()) {
              if (selector.getPattern().equalsIgnoreCase(inputEntry.getPattern()))
                  matched = true;
           }
           for (FileSetConfig.ComponentSelector selector : config.getDirSelectors()) {
               if (selector.getPattern().equalsIgnoreCase(inputEntry.getPattern()))
                   matched = true;
           }
           if (matched)
               matchingConfigs.add(config);
       }

       return Collections.unmodifiableList(matchingConfigs);
   }
}
