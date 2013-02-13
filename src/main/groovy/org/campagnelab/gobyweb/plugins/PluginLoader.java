package org.campagnelab.gobyweb.plugins;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.plugins.xml.CONFIGS_TO_CLASSES;
import org.campagnelab.gobyweb.plugins.xml.Config;
import org.campagnelab.gobyweb.plugins.xml.common.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.common.Need;
import org.campagnelab.gobyweb.plugins.xml.common.Resource;
import org.campagnelab.gobyweb.plugins.xml.common.ResourceConsumerConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 *  The plugin configurations loader
 * @author Fabien Campagne
 * @author Manuele
 */
class PluginLoader {


    private static Logger LOG = Logger.getLogger(PluginLoader.class);

    /**
     * the root XML Schema
     */
    private static final String SCHEMA = "/schemas/plugins.xsd";

    /**
     * Directories under serverConfig that are scanned for configurations
     */
    private static final String[] SCANNED_DIRS = new String[] {
            "/plugins/resources",   // note that we define resources first, before any plugin that may require them.
            "/plugins/aligners",
            "/plugins/analyses"
    };

    /**
     * List of files/dirs ignored by the loader when reading SCANNED_DIRS
     */
    private static final String[] IGNORED_FILES = new String[] {
            "environment.sh",
            ".svn",
            ".DS_Store"
    };

    /**
     * indicates whether the configuration has been already loaded.
     */
    private boolean loaded;
    /**
     * Set to true after loading plugins if some plugin failed validation.
     */
    private Boolean somePluginReportedErrors;

    private File schemaFile;

    private PluginRegistry pluginConfigs = PluginRegistry.getRegistry();

    private List<String> serverConfDirectories = new ArrayList<String>();

    private JAXBContext jaxbContext = null;

    private Marshaller marshaller;

    private Unmarshaller unmarshaller;

    private String webServerHostname;

    PluginLoader() {
    }

    PluginLoader(String serverConf) throws  Exception {
        addServerConf(serverConf);
    }


    PluginLoader(final boolean loadOnCreation) {
        this();
        if (loadOnCreation) {
            //reload();
        }
    }

    /**
     * Define the location of a server-conf directory, where plugins configuration information is stored.
     * Server conf directories will be looked up in the order they are added.
     * Side effect: plugin configuration is reloaded from disk after adding the new server-conf.
     * @param serverConfDirectory Directory with a plugins sub-directory.
     */
    public void addServerConf(String serverConfDirectory) throws  Exception {
        File confDir = new File(serverConfDirectory);

        if (confDir.exists() && confDir.isDirectory()) {

            serverConfDirectories.add(serverConfDirectory);
            locateSchema();
        } else {
            LOG.warn("serverConf does not exist or is not a directory: " + serverConfDirectory);
        }
    }


    /**
     * Reloads plugin configurations from disk.
     */
    public void reload() throws Exception {
        if (webServerHostname == null)
            throw  new Exception("webServerHostname field must not be null.");
        somePluginReportedErrors = false;
        pluginConfigs.clear();
        readConfiguration();
    }

    private void locateSchema() throws  Exception{
        if (schemaFile == null) {
            for (String confDir : serverConfDirectories) {
                final File currentSchemaFile = new File(confDir + SCHEMA);
                if (currentSchemaFile.exists()) {
                    schemaFile = currentSchemaFile;
                    //LOG.info "Installing schema file ${schemaFile} for plugins XML validation."
                    installSchema(currentSchemaFile);
                    return;
                }
            }
            LOG.error ("XML schema is not available for XML validation.") ;
        }
    }

    private void installSchema(File schemaFile) throws  Exception {
        //we install here subclasses to avoid
        //to list in BaseConfig all its sub-classes with the XMLSeeAlso annotation
        Class<?>[] classes = new Class<?>[CONFIGS_TO_CLASSES.values().length];
        int i=0;
        for (CONFIGS_TO_CLASSES value : CONFIGS_TO_CLASSES.values())  {
            classes[i++] = value.register();
        }
        jaxbContext = JAXBContext.newInstance(classes);

        Schema schema;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (schemaFile.exists()) {
            schema = schemaFactory.newSchema(schemaFile);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
            marshaller.setSchema(schema);
            unmarshaller.setSchema(schema);
        }
    }

    public void setWebServerHostname(String webServerHostname) {
        this.webServerHostname = webServerHostname;
    }

    private void readConfiguration() {
        for (String serverConfDir : serverConfDirectories) {
            for (String dir : SCANNED_DIRS)
                readConfigurationFromLocation(serverConfDir + dir);
        }
        // now that all configurations are loaded, trigger post load activities
        for (Config config : pluginConfigs) {
            config.loadCompletedEvent();
        }
        // Check that plugin identifiers are unique across all types of plugins, except resource plugins:
        Object2IntMap idCount = new Object2IntArrayMap();
        idCount.defaultReturnValue(0);
        for (Config config :pluginConfigs) {
            String idUnique = config.getId();
            idCount.put(idUnique,idCount.getInt(idUnique) + 1);
        }

        for (Config config :pluginConfigs) {
            String idUnique = config.getId();
            if (idCount.getInt(idUnique) > 1) {
                for (Config sameIdConfig : pluginConfigs.findAllById(idUnique)) {
                    if (! sameIdConfig.getClass().isAssignableFrom(ResourceConfig.class)) {
                        LOG.error ("Plugin identifier "+ idUnique + "cannot be used more than once");
                        somePluginReportedErrors = true;
                        // decrement the counter so we don't report the error more than once
                        idCount.put(config.getId(), 1);
                    }
                }
            }
        }

        // add GLOBAL resource definition for those plugins that don't provide it explicitly:
        addDefaultNeed("GLOBAL", "excl", "false");
        addDefaultNeed("GLOBAL", "h_vmem", "2g");
        addDefaultNeed("GLOBAL", "virtual_free", "4g") ;
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "excl", "false");
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "h_vmem", "10g");
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "virtual_free", "12g");
        //we are done
        loaded = true;
    }

    /**
     * Add a default value to each plugin when key is not defined for the scope.
     * @param scope
     * @param key
     * @param defaultValue
     */
    private void addDefaultNeed(String scope, String key, String defaultValue) {
        for (ExecutableConfig executableConfig : pluginConfigs.filterConfigs(ExecutableConfig.class)) {
            boolean found = false;
            for (Need need : executableConfig.getRuntime().needs()) {
                 if (need.scope == scope && need.key == key) {
                     found = true;
                     break;
                 }
            }
            // add default value when no value was defined for key in scope:
            if (!found)
                executableConfig.getRuntime().needs().add(new Need(scope, key, defaultValue));
        }
    }


    /**
     * Reads plugin configuration from the specified path.
     * @param location path to load from.
     */
    public void readConfigurationFromLocation(String location) {

        File directory = new File(location);
        if (directory == null || directory.list() == null) {
            LOG.info(String.format("Scanned location %s but ignored since empty.", location));
            return;
        }
        for (String filename : directory.list()) {
            if (ignoreFilenames(filename)) {
                continue;
            }
            readPluginConfigFile(new File(directory, filename).getAbsolutePath());
        }

        // now check resources requirements, and remove the plugins that cannot find their resources:
        List<Config> toRemove = new ArrayList<Config>();
        for (Config config : pluginConfigs) {
            List<String> errors = new ArrayList<String>() ;
            //errors = checkRequiredResources(config, errors);
            if (!errors.isEmpty()) {
                toRemove.add(config);
               /* errors.each { message ->
                        LOG.error("An error occured resolving a plugin resource requirement: ${message}")
                }*/
                for (String error : errors)
                    LOG.error("An error occured resolving a plugin resource requirement:" + error);

            }
        }
        pluginConfigs.removeAll(toRemove);

    }

    private boolean ignoreFilenames(String filename) {
        for (String ignore : IGNORED_FILES) {
            if (ignore.equals(filename))
                return true;
        }
        return false;
    }

    public boolean somePluginReportedErrors() {
        return somePluginReportedErrors;
    }


    private void readPluginConfigFile(String pluginConfigFilePath) {
        // printf "Reading plugin dir: error=%b %s %n",somePluginReportedErrors(), pluginDirectory

        //LOG.info("Scanning location ${pluginConfigFilePath}");
        javax.xml.bind.util.ValidationEventCollector validationCollector;
        Config config;
        List<String> errors;
        java.io.File fileToUnmarshal;
        /*(config, validationCollector, errors, fileToUnmarshal) = parsePluginConfig(pluginConfigFilePath, this);
        if (config == null) {
            return;
        }

        if (!validationCollector.hasEvents()) {
            if (config != null) {
                config.validate(errors);
            }
            if (errors.isEmpty()) {
                FilenameUtils dirName = FilenameUtils.getBaseName(pluginConfigFilePath)
                if (!config.getId().equals(dirName.toString()) && !(config instanceof ResourceConfig)) {
                    // TODO: consider removing this constraint. Non resource plugins would also benefit from keeping older versions around.
                    // checking the name of
                    errors.add(String.format("FATAL: the plugin id %d must match the directory name where the config file resides (%s)", config.getId(), dirName));
                }
                config.setDirectory(pluginConfigFilePath);
                config.configFileReadEvent();
                addServerSidetools(config);
                //accept the configuration
                pluginConfigs.add(config);
                LOG.info(String.format("Registering plugin %s", config.getId()));
            }
        } else {

            for (ValidationEvent event : validationCollector.getEvents()) {
                String msg = event.getMessage();
                ValidationEventLocator locator = event.getLocator();
                int line = locator.getLineNumber();
                int column = locator.getColumnNumber();
                errors.add("An XML validation error was encountered at in file ${fileToUnmarshal} line ${line} column: ${column}, ${msg}  ");
            }
        }
           */
        /*if (!errors.isEmpty()) {
            for (String error : errors) {
                LOG.error("An error occurred configuring plugin: " + error);
            }
            somePluginReportedErrors = true;
        }  */
    }

    /**
     * Check that pluginConfig's resources are available in this instance of GobyWeb.
     * @param pluginConfig
     * @param errors
     * @return
     */
    ArrayList<String> checkRequiredResources(ResourceConsumerConfig pluginConfig, ArrayList<String> errors) {
        for (Resource resourceRef : pluginConfig.requires) {
            // write resources in the format  ${ RESOURCES _ resource-id _ file-id}
            ResourceConfig resource = lookupResource(resourceRef.id, resourceRef.versionAtLeast, resourceRef.versionExactly);
            if (resource == null) {
                errors = (errors == null ? new ArrayList<String>() : errors);
                if (resourceRef.versionExactly != null) {
                    errors.add(String.format("Plugin %s requires resource %s versionExactly %s, but this resource is not available. ",
                            pluginConfig.getId(), resourceRef.id, resourceRef.versionExactly));
                } else if (resourceRef.versionAtLeast != null) {
                    errors.add(String.format("Plugin %s requires resource %s versionAtLeast %s, but this resource is not available. ",
                            pluginConfig.getId(), resourceRef.id, resourceRef.versionAtLeast));
                }
            }
        }
        return errors;
    }

    /**
     * Return the resource with largest version number, such that the resource has the identifier and at least the specified
     * version number.
     * @param resourceId
     * @param versionExactly required version
     * @return Most recent resource (by version number) with id and version>v
     */
    ResourceConfig lookupResource(String resourceId, String versionAtLeast, String versionExactly) {
        List<ResourceConfig> resourceList = pluginConfigs.filterConfigs(ResourceConfig.class);
        for (ResourceConfig resource: resourceList) {
         /*   if (versionExactly != null) {
                resource instanceof ResourceConfig &&
                        resource.getId() == resourceId &&
                        resource.exactlyVersion(versionExactly)
            } else if (versionAtLeast != null) {
                resource instanceof ResourceConfig &&
                        resource.getId() == resourceId &&
                        resource.atLeastVersion(versionAtLeast)
            }  */
        }
        //resourceList = resourceList.sort { a, b -> (a.atLeastVersion(b.version) ? -1 : +1) }
        return resourceList.get(0);
    }


    /**
     * Adds dependency on GOBYWEB_SERVER_SIDE resource plugin on each non resource plugins.
     */
    private void addServerSidetools(Config config) {
        if (config instanceof ExecutableConfig) {
            ExecutableConfig executableConfig = (ExecutableConfig) config;
            ResourceConfig resource = lookupResource("GOBYWEB_SERVER_SIDE", "2.0", null);
            assert resource != null: " The GOBYWEB_SERVER_SIDE plugin resource must exist";
            Resource resourceRef = new Resource();
            resourceRef.id = resource.getId();
            resourceRef.versionExactly = resource.getVersion();
            if (!executableConfig.requires.contains(resourceRef)) {
                executableConfig.requires.add(0, resourceRef);
            }
        }

    }
}
