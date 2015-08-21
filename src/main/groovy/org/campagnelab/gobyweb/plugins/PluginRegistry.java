package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  The plugin registry. Holds a set of GobyWeb plugins and provides methods to find plugins in the registry.
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
     * Gets a registry, not the singleton registry.
     * @return the plugin registry
     */
    public static PluginRegistry getARegistry() {
        return new PluginRegistry();
    }
    /**
     * Gets the list of configurations for a given plugin type
     * @param configClass the type of configurations
     * @param <T> the class to filter for
     * @return the list of configurations for the plugin type loaded from the disk
     */
    public synchronized <T extends Config> List<T> filterConfigs(Class<T> configClass) {
        List<T> returnedList = new ArrayList<T>();
        for (Config p : this) {
            if ( (p.getClass().isAssignableFrom(configClass) //same class
                || (configClass.isInstance(p))))             //or a sub-class
                returnedList.add((T) p);
        }
        return returnedList;
    }

    /**
     * Returns the configuration matching id or null if the configuration was not found.
     * @param idToFind
     * @return the configuration that matches or null
     */
    public synchronized Config findById(String idToFind) {
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
    public synchronized List<Config> findAllById(String idToFind) {
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
     * @param <T> the class to filter for
     * @return the configuration that matches or null
     */
    public synchronized <T extends Config> T findByTypedId(String idToFind, Class<T> configClass ) {
        return findByTypedIdAndVersion(idToFind, null, configClass);
    }


    /**
     * Returns the configuration matching id or null if the configuration was not found.
     * @param idToFind
     * @param version
     * @param configClass the type of configurations
     * @param <T> the class to filter for
     * @return the configuration that matches or null
     */
    public synchronized  <T extends Config> T findByTypedIdAndVersion(String idToFind, String version, Class<T> configClass ) {
        if (idToFind != null) {
            for (Config config: this) {
                if ((config.getId().compareTo(idToFind)==0) && (!config.isDisabled())
                        && ((config.getClass().isAssignableFrom(configClass)) //same class
                        ||(configClass.isInstance(config)))){  //or a sub-class
                    if ((version == null) || (config.getVersion().equalsIgnoreCase(version)))
                     return (T)config;
                }
            }
        }
        return null;
    }

    public synchronized boolean add(Config config) {
        return super.add(config);
    }

    public synchronized boolean remove(Config config) {
        return super.remove(config);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> configs) {
        return super.removeAll(configs);
    }

    public synchronized boolean addAll(Collection<? extends Config> configs) {
        return super.addAll(configs);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

}

