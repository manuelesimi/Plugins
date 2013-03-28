package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds a fileset instance configuration that matches the input entry.
 *
 *  @author manuele
 */
class FileSetConfigMatcher {

   protected FileSetConfigMatcher(InputEntry inputEntry ){

   }

   protected List<FileSetConfig> getConfigurations() {
       List<FileSetConfig> configs = new ArrayList<FileSetConfig>();

       return configs;
   }
}
