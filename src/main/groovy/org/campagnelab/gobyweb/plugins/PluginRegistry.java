package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *  The plugin registry
 *  @author manuele
 *
 */
public class PluginRegistry extends ArrayList<Config> {

    /**
     * singleton instance of the registry created at startup time
     */
    private final static PluginRegistry instance = new PluginRegistry();

    private PluginRegistry() {
        super();
    }

    private PluginRegistry(int i) {
        super(i);
    }

    private PluginRegistry(Collection<? extends Config> configs) {
        super(configs);
    }

    /**
     * Gets the registry
     * @return the plugin registry
     */
    public static PluginRegistry getRegistry() {
        return instance;
    }

    /**
     * Gets the list of configurations for a given plugin type
     * @param configClass the type of configurations
     * @return the list of configurations for the plugin type loaded from the disk
     */
    public <T extends Config> List<T> filterConfigs(Class<T> configClass) {
        List<T> returnedList = new ArrayList<T>();
        for (Config p : this) {
            if (p.getClass().isAssignableFrom(configClass))
                returnedList.add((T) p);
        }
        return Collections.unmodifiableList(returnedList);
    }

    /**
     * Returns the configuration matching id or null if the configuration was not found.
     * @param idToFind
     * @return the configuration that matches or null
     */
    public Config findById(String idToFind) {
        if (idToFind != null) {
            for (Config config: this) {
                if (config.getId().compareTo(idToFind)==0) {
                    return config;
                }
            }
        }
        return null;
    }

    /**
     * Returns all the configurations matching id or an empty list if any configuration was not found.
     * @param idToFind
     * @return the configurations that match or null
     */
    public List<Config> findAllById(String idToFind) {
        List<Config> returnedList = new ArrayList<Config>();
        if (idToFind != null) {
            for (Config config: this) {
                if (config.getId().compareTo(idToFind)==0) {
                    returnedList.add(config);
                }
            }
        }
        return returnedList;
    }

    /**
     * Returns the configuration matching id or null if the configuration was not found.
     * @param idToFind
     * @param configClass the type of configurations
     * @param <T>
     * @return the configuration that matches or null
     */
    public <T extends Config> T findByTypedId(String idToFind, Class<T> configClass ) {
        if (idToFind != null) {
            for (Config config: this) {
                if ((config.getId().compareTo(idToFind)==0)
                        && (config.getClass().isAssignableFrom(configClass))){
                    return (T)config;
                }
            }
        }
        return null;
    }

}

