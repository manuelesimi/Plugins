/*
 * Copyright (c) 2011  by Cornell University and the Cornell Research
 * Foundation, Inc.  All Rights Reserved.
 *
 * Permission to use, copy, modify and distribute any part of GobyWeb web
 * application for next-generation sequencing data alignment and analysis,
 * officially docketed at Cornell as D-5061 ("WORK") and its associated
 * copyrights for educational, research and non-profit purposes, without
 * fee, and without a written agreement is hereby granted, provided that
 * the above copyright notice, this paragraph and the following three
 * paragraphs appear in all copies.
 *
 * Those desiring to incorporate WORK into commercial products or use WORK
 * and its associated copyrights for commercial purposes should contact the
 * Cornell Center for Technology Enterprise and Commercialization at
 * 395 Pine Tree Road, Suite 310, Ithaca, NY 14850;
 * email:cctecconnect@cornell.edu; Tel: 607-254-4698;
 * FAX: 607-254-5454 for a commercial license.
 *
 * IN NO EVENT SHALL THE CORNELL RESEARCH FOUNDATION, INC. AND CORNELL
 * UNIVERSITY BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL,
 * OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF
 * WORK AND ITS ASSOCIATED COPYRIGHTS, EVEN IF THE CORNELL RESEARCH FOUNDATION,
 * INC. AND CORNELL UNIVERSITY MAY HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE WORK PROVIDED HEREIN IS ON AN "AS IS" BASIS, AND THE CORNELL RESEARCH
 * FOUNDATION, INC. AND CORNELL UNIVERSITY HAVE NO OBLIGATION TO PROVIDE
 * MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.  THE CORNELL
 * RESEARCH FOUNDATION, INC. AND CORNELL UNIVERSITY MAKE NO REPRESENTATIONS AND
 * EXTEND NO WARRANTIES OF ANY KIND, EITHER IMPLIED OR EXPRESS, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A
 * PARTICULAR PURPOSE, OR THAT THE USE OF WORK AND ITS ASSOCIATED COPYRIGHTS
 * WILL NOT INFRINGE ANY PATENT, TRADEMARK OR OTHER RIGHTS.
 */

package org.campagnelab.gobyweb.plugins

import com.google.common.io.Files
import edu.cornell.med.icb.util.ICBStringUtils
import it.unimi.dsi.fastutil.objects.*
import it.unimi.dsi.lang.MutableString
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.campagnelab.gobyweb.artifacts.Artifacts
import org.campagnelab.gobyweb.artifacts.BuildArtifactRequest
import org.campagnelab.gobyweb.plugins.xml.*
import org.campagnelab.gobyweb.plugins.xml.executables.Need
import org.campagnelab.gobyweb.plugins.xml.executables.Option
import org.campagnelab.gobyweb.plugins.xml.executables.Option.OptionType
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile
import org.campagnelab.gobyweb.plugins.xml.resources.Resource
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig
import org.campagnelab.gobyweb.plugins.xml.executables.Script
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig
import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.*;

import javax.xml.XMLConstants
import javax.xml.bind.*
import javax.xml.bind.util.ValidationEventCollector
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

/**
 * Support for loading Plugins.
 * @author Fabien Campagne
 * Date: 10/12/11
 * Time: 11:11 AM
 *
 */
public class Plugins {

    private static Logger LOG = Logger.getLogger(Plugins.class);

    /**
     * indicates whether the configuration has been already loaded.
     */
    private boolean loaded;
    /**
     * Set to true after loading plugins if some plugin failed validation.
     */
    private Boolean somePluginReportedErrors;

    private String pluginErrorMessage = "";

    private File schemaFile;

    /**
     * the registry of the valid plugin configurations loaded from the disk
     */
    private PluginRegistry pluginConfigs = PluginRegistry.getRegistry();

    private List<String> serverConfDirectories = new ArrayList<String>();

    private ArtifactsProtoBufHelper artifactsPbHelper;

    private JAXBContext jaxbContext = null;

    private Marshaller marshaller;

    private Unmarshaller unmarshaller;

    private String webServerHostname;

    Plugins() {
        artifactsPbHelper = new ArtifactsProtoBufHelper()
    }

    Plugins(String serverConf) {
        this()
        addServerConf(serverConf)
    }


    Plugins(final boolean loadOnCreation) {
        this()
        if (loadOnCreation) {
            reload()
        }
    }

    /**
     * Gets the registry of the valid plugin configurations loaded from the disk
     * @return the registry
     */
    public PluginRegistry getRegistry() {
        return this.pluginConfigs;
    }

    /**
     * Reload the plugin configuration from disk.
     */
    public void reload() {
        assert webServerHostname != null: "webServerHostname field must not be null."
        somePluginReportedErrors = false
        pluginConfigs.clear();
        readConfiguration();
        autoOptionsFileHelper = new AutoOptionsFileHelper(pluginConfigs)
    }

    private void readConfiguration() {
        if (schemaFile == null)
            throw new IllegalStateException("No valid schema file is available for validating plugin configuration")
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
        for (Config config : pluginConfigs) {
            String idUnique = config.getId();
            idCount.put(idUnique, idCount.getInt(idUnique) + 1);
        }

        for (Config config : pluginConfigs) {
            String idUnique = config.getId();
            if (idCount.getInt(idUnique) > 1) {
                for (Config sameIdConfig : pluginConfigs.findAllById(idUnique)) {
                    if (!sameIdConfig.getClass().isAssignableFrom(ResourceConfig.class)) {
                        LOG.error("Plugin identifier " + idUnique + " cannot be used more than once");
                        pluginErrorMessage = "Plugin identifier " + idUnique + " cannot be used more than once";
                        somePluginReportedErrors = true;
                        // decrement the counter so we don't report the error more than once
                        idCount.put(config.getId(), 1);
                    }
                }
            }
        }
        ObjectArrayList<String> errors = new ObjectArrayList<String>();


        for (ResourceConfig resourceConfig : pluginConfigs.filterConfigs(ResourceConfig.class)) {
            resourceConfig.validateFiles(errors);
        }

        for (String error : errors) {
            somePluginReportedErrors = true;
            LOG.error(String.format("plugin root=%s %s", ObjectArrayList.wrap(serverConfDirectories).toString(), error));
            pluginErrorMessage = String.format("plugin root=%s %s", ObjectArrayList.wrap(serverConfDirectories).toString(), error);
        }
        // add GLOBAL resource definition for those plugins that don't provide it explicitly:
        addDefaultNeed("GLOBAL", "excl", "false");
        addDefaultNeed("GLOBAL", "h_vmem", "6g");
        //addDefaultNeed("GLOBAL", "virtual_free", "4g");
        addDefaultNeed("DEFAULT_JVM_OPTIONS", "", "-Xms40m -Xmx250m");
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "excl", "false");
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "h_vmem", "10g");
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "virtual_free", "12g");
        //we are done
        loaded = true;
    }

    /**
     * Register environment collection scripts.
     * @param script
     */
    public void registerPluginEnvironmentCollectionScript(String script) {
        artifactsPbHelper.registerPluginEnvironmentCollectionScript(script)
    }

    /**
     * Add a default value to each plugin when key is not defined for the scope.
     * @param executablePluginConfig
     * @param scope
     * @param key
     * @param defaultValue
     */
    private void addDefaultNeed(String scope, String key, String defaultValue) {
        for (ExecutableConfig executableConfig : pluginConfigs.filterConfigs(ExecutableConfig.class)) {
            if (executableConfig.getRuntime().needs().findAll { need ->
                need.scope == scope && need.key == key
            }.isEmpty()) {
                // add default value when no value was defined for key in scope:
                executableConfig.getRuntime().needs().add(new Need(scope, key, defaultValue))
            }
        }
    }

    /**
     * Returns a string that describes registered plugins.
     * @return human readable plugins description.
     */
    public String describePlugins() {
        MutableString buffer = new MutableString();
        for (Config config : pluginConfigs)
            buffer.append(String.format(" %s\n", config.toString()));

        return buffer.toString();
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
            def pluginDir = new File(directory, filename)
            if (pluginDir.isDirectory()) {
                readPluginConfigFile(pluginDir.getAbsolutePath());
            }
        }

        // now check resources requirements, and remove the plugins that cannot find their resources:
        def toRemove = []
        for (Config config : pluginConfigs) {
            if ((config.getClass().isAssignableFrom(ResourceConsumerConfig.class) //same class
                    || (ResourceConsumerConfig.isInstance(config)))) {            //or a sub-class
                LOG.trace("Checking resources for ${config}")
                def errors = new ArrayList<String>()
                errors = checkRequiredResources(config, errors)
                if (!errors.isEmpty()) {
                    toRemove.add(config)
                    errors.each { message ->
                        LOG.error("An error occurred resolving a plugin resource requirement: ${message}")
                    }
                }
            }
        }
        pluginConfigs.removeAll(toRemove)

    }

    public boolean somePluginReportedErrors() {
        somePluginReportedErrors
    }

    public String getPluginReportedErrors() {
        pluginErrorMessage
    }

    private boolean ignoreFilenames(String filename) {
        for (String ignore : IGNORED_FILES) {
            if (ignore.equals(filename))
                return true;
        }
        return false;
    }


    private void readPluginConfigFile(String pluginConfigFilePath) {
        // printf "Reading plugin dir: error=%b %s %n",somePluginReportedErrors(), pluginDirectory

        LOG.info("Scanning location ${pluginConfigFilePath}");
        javax.xml.bind.util.ValidationEventCollector validationCollector
        Config config
        java.util.List<java.lang.String> errors
        java.io.File fileToUnmarshal
        (config, validationCollector, errors, fileToUnmarshal) = parsePluginConfig(pluginConfigFilePath, this)
        if (config == null) {
            return
        }

        if (!validationCollector.hasEvents()) {  //the config was correctly loaded

            if (config != null) {

                config.validate(errors);
            }
            if (errors.isEmpty()) {
                def dirName = FilenameUtils.getBaseName(pluginConfigFilePath)
                if (!config.id.equals(dirName) && !(config instanceof ResourceConfig)) {
                    // TODO: consider removing this constraint. Non resource plugins would also benefit from keeping older versions around.
                    // checking the name of

                    errors.add(String.format("FATAL: the plugin id %s must match the directory name where the config file resides (%s)",
                            config.id, dirName));
                }
                config.setDirectory(pluginConfigFilePath);
                config.configFileReadEvent();

                addServerSidetools(config)
                //accept the configuration
                pluginConfigs.add(config);
                LOG.info(String.format("Registering plugin %s", config.id));
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

        if (!errors.isEmpty()) {
            errors.each { message ->
                println("An error occured configuring plugin: ${message}")
                LOG.error("An error occured configuring plugin: ${message}")
                pluginErrorMessage += "An error occured configuring plugin: ${message}";
            }
            somePluginReportedErrors = true
        }
    }

    public void saveToXml(Config config, OutputStream os) {

        marshaller.marshal(config, os)
        os.flush()
    }

    public List parsePluginConfig(String pluginConfigFilePath, Plugins pluginSystem) {

        //If the message regarding validation to be customized
        ValidationEventCollector validationCollector = new ValidationEventCollector();
        unmarshaller.setEventHandler(validationCollector);
        Config config
        List<String> errors = new ArrayList<String>()
        final File fileToUnmarshal = new File(pluginConfigFilePath, "config.xml");
        if (!fileToUnmarshal.exists()) {
            config = null;
        }

        try {
            config = (Config) unmarshaller.unmarshal(fileToUnmarshal);
            if (config == null) {
                errors.add("Cannot find config.xml file in plugin directory " + fileToUnmarshal);
                LOG.error "Cannot find config.xml file in plugin directory " + fileToUnmarshal
            }
        } catch (JAXBException e) {
            // Errors will be reported below, no need to log them here
            if (pluginSystem != null) {
                pluginSystem.somePluginReportedErrors = true
            }
            pluginErrorMessage = "JAXBException when unmarshaling the plugin in $pluginConfigFilePath";
            errors.add(e.getMessage())
            LOG.error("JAXBException when unmarshaling a plugin", e)
        }
        [config, validationCollector, errors, fileToUnmarshal]
    }

    /**
     * Adds dependency on SERVER_SIDE_TOOL resource plugin on each ExecutableConfig plugin.
     */
    private void addServerSidetools(Config config) {
        if (config instanceof ExecutableConfig) {
            ResourceConfig resource = lookupResource(SERVER_SIDE_TOOL[0], SERVER_SIDE_TOOL[1], SERVER_SIDE_TOOL[2])
            assert resource != null: " The ${SERVER_SIDE_TOOL[0]} resource must exist";
            Resource resourceRef = new Resource()
            resourceRef.id = resource.id
            resourceRef.versionExactly = resource.version
            if (!config.requires.contains(resourceRef)) {
                config.requires.add(0, resourceRef);
            }
        }

    }

    /**
     * Check that pluginConfig's resources are available in this instance of GobyWeb.
     * @param pluginConfig
     * @param errors
     * @return
     */
    List<String> checkRequiredResources(ResourceConsumerConfig pluginConfig, ArrayList<String> errors) {
        for (Resource resourceRef : pluginConfig.requires) {
            // write resources in the format  ${ RESOURCES _ resource-id _ file-id}
            ResourceConfig resource = lookupResource(resourceRef.id, resourceRef.versionAtLeast, resourceRef.versionExactly)
            if (resource == null) {
                errors = (errors == null ? new ArrayList<String>() : errors)
                if (resourceRef.versionExactly != null) {
                    errors.add(String.format("Plugin %s requires resource %s versionExactly %s, but this resource is not available. ",
                            pluginConfig.id, resourceRef.id, resourceRef.versionExactly));
                } else if (resourceRef.versionAtLeast != null) {
                    errors.add(String.format("Plugin %s requires resource %s versionAtLeast %s, but this resource is not available. ",
                            pluginConfig.id, resourceRef.id, resourceRef.versionAtLeast));
                }
            }
        }
        return errors;
    }

    /**
     * Given a plugin, make a map of the plugin and resource names, to their version numbers.
     * @param pluginConfig the plugin to collect version numbers for
     * @return map of plugins to version numbers
     */
    Map<String, String> pluginVersionsMap(ResourceConsumerConfig config) {
        pluginVersionsMap(config, new LinkedHashMap<String, String>())
    }
    /**
     * Given a plugin, make a map of the plugin and resource names, to their version numbers.
     * @param pluginConfig the plugin to collect version numbers for
     * @return map of plugins to version numbers
     */

    Map<String, String> pluginVersionsMap(ResourceConsumerConfig pluginConfig, Map<String, String> versionsMap) {
        versionsMap["${pluginConfig.getClass().getName()}:${pluginConfig.id}:${pluginConfig.name}"] =
            pluginConfig.version
        for (Resource resource in pluginConfig.requires) {
            ResourceConfig resourceConfig = lookupResource(
                    resource.id, resource.versionAtLeast, resource.versionExactly)
            versionsMap["${resourceConfig.getClass().getName()}:${resourceConfig.id}:${resourceConfig.name}"] =
                resourceConfig.version
            pluginVersionsMap(resourceConfig, versionsMap)
        }
        return versionsMap
    }
    /**
     * Given a map of plugins to version numbers created by pluginVersionsMap
     * create a list of map, where each map contains details about the plugins
     * used, their names, version number, id, and the class for that plugin
     * @param versionsMap the version map created by pluginVersionsMap
     * @return list of map of details for the used plugins
     */
    List<Map<String, String>> pluginVersionsMapToDisplayList(Map<String, String> versionsMap) {
        List<Map<String, String>> displayList = new LinkedList<Map<String, String>>()
        if (versionsMap) {
            versionsMap.each { String k, String pluginVersion ->
                def item = [:]
                try {
                    def (pluginClassName, pluginId, pluginName) = k.split(":", 3)
                    item.className = pluginClassName
                    item.id = pluginId
                    item.name = pluginName
                    item.version = pluginVersion
                    displayList << item
                } catch (ArrayIndexOutOfBoundsException e) {
                    // Skip this entry. bad format.
                }
            }
        }
        return displayList
    }

    public void setWebServerHostname(String webServerHostname) {
        this.webServerHostname = webServerHostname;
        artifactsPbHelper.setWebServerHostname(webServerHostname)
    }
/**
 * Define the location of a server-conf directory, where plugins configuration information is stored.
 * Server conf directories will be looked up in the order they are added.
 * Side effect: plugin configuration is reloaded from disk after adding the new server-conf.
 * @param serverConfDirectory Directory with a plugins sub-directory.
 */
    public void addServerConf(String serverConfDirectory) {
        File confDir = new File(serverConfDirectory)
        if (confDir.exists() && confDir.isDirectory()) {
            serverConfDirectories.add(serverConfDirectory);
            locateSchema()
        } else {
            LOG.warn("Plugin root does not exist or is not a directory: " + serverConfDirectory)
            somePluginReportedErrors = true;
            pluginErrorMessage = "Plugins root does not exist or is not a directory: " + serverConfDirectory;
        }
    }

    /**
     *
     * @param schemaConfigDirectory
     */
    public void replaceDefaultSchemaConfig(String schemaConfigDirectory) {
        final File currentSchemaFile = new File(schemaConfigDirectory + SCHEMA);
        if (currentSchemaFile.exists()) {
            schemaFile = currentSchemaFile
            LOG.info "Installing schema file ${schemaFile} for plugins XML validation."
            installSchema(currentSchemaFile)
        }
    }

    private void locateSchema() {
        if (schemaFile == null) {
            for (String confDir in serverConfDirectories) {
                final File currentSchemaFile = new File(confDir + SCHEMA);
                if (currentSchemaFile.exists()) {
                    schemaFile = currentSchemaFile
                    LOG.info "Installing schema file ${schemaFile} for plugins XML validation."
                    installSchema(currentSchemaFile)
                    return
                }
            }
            LOG.error "XML schema is not available for XML validation."
        }
    }

    def installSchema(File schemaFile) {
        //we install here subclasses to avoid
        //to list in BaseConfig all its sub-classes with the XMLSeeAlso annotation
        Class<?>[] classes = new Class<?>[CONFIGS_TO_CLASSES.values().length];
        int i = 0;
        for (CONFIGS_TO_CLASSES value : CONFIGS_TO_CLASSES.values()) {
            classes[i++] = value.register()
        }
        jaxbContext = JAXBContext.newInstance(classes);
        Schema schema;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        if (schemaFile?.exists()) {
            schema = schemaFactory.newSchema(schemaFile);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
            marshaller.setSchema(schema);
            unmarshaller.setSchema(schema);
        }
    }

    /**
     * Returns the PluginConfig matching dbLegacyId or null if the plugin was not found.
     * @param type the type of Config object to find as it is possible dbLegacyId's can clash across types
     * @param idToFind the id to find
     * @return the matching PluginConfig or null
     */
    public Config findByDbLegacyId(Class type, String idToFind) {
        if (idToFind) {
            for (Config plugin in pluginConfigs) {
                if (((plugin.getClass().isAssignableFrom(type)) //same class
                        || (type.isInstance(plugin)))  //or a sub-class
                        && (plugin.dbLegacyId && plugin.dbLegacyId == idToFind)) {
                    return plugin
                }
            }
        }
        return null;
    }


    AutoOptionsFileHelper autoOptionsFileHelper;

    /**
     * Return the resource with largest version number, such that the resource has the identifier and at least the specified
     * version number.
     * @param resourceId
     * @param v required version
     * @return Most recent resource (by version number) with id and version>v
     */
    ResourceConfig lookupResource(String resourceId, String versionAtLeast, String versionExactly) {
        return DependencyResolver.resolveResource(resourceId, versionAtLeast, versionExactly);

    }

    /**
     * Returns the id of the plugin this plugin's script is imported from, or null if script was not imported.
     * @param config A plugin configuration.
     * @return Id of a plugin or null.
     */
    public static String scriptImportedFrom(PluginFileProvider config) {
        def value = config.files.find({ pluginFile ->
            "SCRIPT".equals(pluginFile.id) && (pluginFile.importFromPlugin != null)
        })
        value == null ? null : value.importFromPlugin;
    }
    /**
     * Return a map structured as follows, with isVisibleWhen obtained from converting the option validation language expressions
     *  to javascript expressions:
     <pre>
     var gobyConfig = {"plugin-id1_option_id1": {"itemType": "select",
     "isVisible": true,
     "currentValue": "",},
     "plugin-id1_option_id2": {"itemType": "select",
     "isVisible": true,
     "currentValue": ""},
     "plugin-id1_option_id1": {"itemType": "checkbox",
     "isVisible": true,
     "currentValue": "",
     "isVisibleWhen": "gobyConfig.fish.currentValue=='goby'"},
     ...};
     </pre>
     * @param typeOfPlugin
     * @return
     */
    public HashMap<String, HashMap<String, Object>> javaScriptMap(Class typeOfPlugin) {
        HashMap<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
        Object2BooleanOpenHashMap includeOptionInMap = new Object2BooleanOpenHashMap()

        // Determine which options should be listed in the map. We include options with a hiddenWhen attribute and options
        // these attributes reference.
        List<ExecutableConfig> confs = pluginConfigs.filterConfigs(typeOfPlugin)
        if (confs.size() == 0) {
            LOG.warn("javaScriptMap: no configuration found of type " + typeOfPlugin)
            return
        }
        for (ExecutableConfig conf : confs) {
            conf.options().each { option ->
                if (option.hiddenWhenParsed != null) {
                    String fullId = conf.id + "_" + option.id
                    includeOptionInMap.put(fullId, true)
                    option.hiddenWhenParsed.get().optionIdsList().each {
                        optionIdReferenced ->
                            includeOptionInMap.put(conf.id + "_" + optionIdReferenced, true)
                    }
                }
            }
        }

        for (ExecutableConfig conf : confs) {
            conf.options().each { option ->
                String fullId = conf.getId() + "_" + option.id
                if (includeOptionInMap.get(fullId)) {
                    result.put(fullId, getJavaScriptOptionMap(conf, option))
                }
            }
        }
        return result;
    }

    public Map<String, Object> getJavaScriptOptionMap(Config plugin, Option option) {
        Map<String, Object> result = new HashMap<String, Object>();
        def type = "text";
        if (option.type == OptionType.CATEGORY) type = "select"
        if (option.type == OptionType.SWITCH) type = "checkbox"
        if (option.type == OptionType.BOOLEAN) type = "checkbox"

        result.put("itemType", type)
        result.put("isVisible", true)
        result.put("currentValue", "")
        if (option.hiddenWhenParsed != null) {

            def script = "!(" + option.hiddenWhenParsed.get().produceJavaScript(plugin.id) + ")"
            result.put("isVisibleWhen", script)

            LOG.debug("Produced java script for option " + option.id + ": " + script);
        }

        result
        /*"cars": {
            "itemType": "select",
            "isVisible": true,
            "currentValue": "",
            "isVisibleWhen", "javascript expression in the format gobyConfig. PLUGIN-ID _ OPTION-ID.currentValue == 'OTHER_CATEGORY_ID'
        },
        */
    }

    /**
     * Execute a script.
     * @param pluginConfig the plugin configuration
     * @param script details about the script to execute
     * @param gobywebObj the gobywebObject to pass to the script
     * @return the temp directory where any files the script creates will be stored
     */
    File executeScript(
            final ExecutableConfig pluginConfig,
            final Script script,
            final Object gobywebObj,
            final Map bindings) {
        final File tempDir = null
        if (script.language == "groovy") {
            final File pluginScriptFilename = new File(pluginConfig.getDirectory(), script.filename)
            if (pluginScriptFilename.exists()) {
                tempDir = Files.createTempDir();
                try {
                    final GroovyShell shell = new GroovyShell()
                    groovy.lang.Script pluginScript = shell.parse(pluginScriptFilename)
                    final int returnValue = pluginScript.execute(gobywebObj, tempDir, bindings)
                    if (returnValue != 0) {
                        FileUtils.deleteDirectory(tempDir)
                        tempDir = null
                        LOG.error("Error running script ${script.filename} for tag=${gobywebObj.tag}, return value was ${returnValue}")
                    }
                } catch (Exception e) {
                    // Error running script, remove the tempDir
                    FileUtils.deleteDirectory(tempDir)
                    tempDir = null
                    LOG.error("Error running script ${script.filename} for tag=${gobywebObj.tag}", e)
                }
            } else {
                LOG.error("Script filename ${script.filename} for tag=${gobywebObj.tag} doesn't exist")
            }
        } else {
            LOG.error("Script filename ${script.filename} for tag=${gobywebObj.tag} is in unsupported language ${script.language}")
        }
        return tempDir
    }

    public File createPbRequestFile(ResourceConsumerConfig alignerById) {
        return artifactsPbHelper.createPbRequestFile(alignerById)
    }
    /**
     * Write a bash shell script with environment variable definitions for each automatic plugin option.
     * @param pluginConfig The plugin for which user-defined option values should be written.
     * @return The temporary file where options have been written.
     */
    public File generateAutoOptionsFile(ExecutableConfig pluginConfig, String attributesPrefix = null, Map<String, String> attributes = null) {
        autoOptionsFileHelper.generateAutoOptionsFile(pluginConfig, attributesPrefix, attributes)
    }


}
