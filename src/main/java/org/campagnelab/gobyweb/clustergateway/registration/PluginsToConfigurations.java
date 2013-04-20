package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.configuration.Configuration;
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Converter from plugins to configurations that can be consumed by the FileSetAPI
 *
 * @author manuele
 */
public class PluginsToConfigurations {


    /**
     * Converts the list of plugins to a configuration list for the FileSetAPI
     *
     * @param fileSetConfigs
     * @return
     */
    public static ConfigurationList convertAsList(List<FileSetConfig> fileSetConfigs) {
        ConfigurationList configurationList = new ConfigurationList();
        for (Configuration configuration :
                PluginsToConfigurations.convert(fileSetConfigs)) {
            configurationList.addConfiguration(configuration);
        }
        return configurationList;
    }


    /**
     * Converts the list of plugins to a list of configurations for the FileSetAPI
     *
     * @param fileSetConfigs the plugins to convert
     * @return
     */
    public static List<Configuration> convert(List<FileSetConfig> fileSetConfigs) {
        List<Configuration> configurationList = new ArrayList<Configuration>();
        for (FileSetConfig fileSetConfig : fileSetConfigs)
            configurationList.add(convert(fileSetConfig));
        return configurationList;
    }

    /**
     * Converts the plugin configuration to a configuration for the FileSetAPI
     *
     * @param fileSetConfig the plugin to convert
     * @return
     */
    public static Configuration convert(FileSetConfig fileSetConfig) {
        Configuration configuration = new Configuration(fileSetConfig.getId());
        configuration.setName(fileSetConfig.getName());
        configuration.setVersion(fileSetConfig.getVersion());
        for (FileSetConfig.ComponentSelector selector : fileSetConfig.getFileSelectors()) {
            configuration.addFileSelector(
                    new Configuration.ComponentSelector(selector.getId(), selector.getPattern(), selector.getMandatory())
            );
        }
        for (FileSetConfig.ComponentSelector selector : fileSetConfig.getDirSelectors()) {
            configuration.addDirSelector(
                    new Configuration.ComponentSelector(selector.getId(), selector.getPattern(), selector.getMandatory())
            );
        }
        return configuration;
    }
}
