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

import edu.cornell.med.icb.util.ICBStringUtils
import it.unimi.dsi.fastutil.objects.*
import it.unimi.dsi.lang.MutableString
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.campagnelab.gobyweb.artifacts.Artifacts
import org.campagnelab.gobyweb.artifacts.BuildArtifactRequest
import org.campagnelab.gobyweb.plugins.xml.*
import org.campagnelab.gobyweb.plugins.xml.Option.OptionType
import scala.tools.nsc.dependencies.Files

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

    private ArrayList<PluginConfig> pluginConfigs = new ArrayList<PluginConfig>();

    /**
     * indicates whether the configuration has been already loaded.
     */
    private boolean loaded;
    /**
     * Set to true after loading plugins if some plugin failed validation.
     */
    private Boolean somePluginReportedErrors

    private File schemaFile

    Plugins() {
    }

    Plugins(String serverConf) {
        addServerConf(serverConf)
    }


    Plugins(final boolean loadOnCreation) {
        this()
        if (loadOnCreation) {
            reload()
        }
    }

    public ArrayList<PluginConfig> getAlignerPluginConfigs() {
        return pluginConfigs.findAll({ i ->
            i instanceof AlignerConfig;
        });
    }

    public ArrayList<PluginConfig> getAlignmentAnalysisConfigs() {
        return pluginConfigs.findAll({ i ->
            i instanceof AlignmentAnalysisConfig;
        });
    }

    public ArrayList<PluginConfig> getResourceConfigs() {
        return pluginConfigs.findAll({ i ->
            i instanceof ResourceConfig;
        });
    }

    /**
     * Reload the plugin configuration from disk.
     */
    public void reload() {
        assert webServerHostname != null: "webServerHostname field must not be null."
        somePluginReportedErrors = false
        pluginConfigs.clear();
        readConfiguration();
    }

    private void readConfiguration() {
        for (String serverConfDir : serverConfDirectories) {
            // note that we define resources first, before any plugin that may require them.
            readConfigurationFromLocation(serverConfDir + "/" + "plugins/resources", true);
            readConfigurationFromLocation(serverConfDir + "/" + "plugins/aligners");
            readConfigurationFromLocation(serverConfDir + "/" + "plugins/analyses");
        }
        // now that all plugins are defined, process file plugin imports:
        for (PluginConfig config : pluginConfigs) {
            for (PluginFile pluginFile : config.files) {

                pluginFile.constructLocalFilename(config.pluginDirectory, this);

            }
        }
        // Check that plugin identifiers are unique across all types of plugins, except resource plugins:
        Object2IntMap idCount = new Object2IntArrayMap()
        idCount.defaultReturnValue(0)
        pluginConfigs.each { pluginConfig ->
            def idUnique = pluginConfig.id
            idCount[idUnique] = idCount.getInt(idUnique) + 1
        }
        pluginConfigs.each { pluginConfig ->
            def idUnique = pluginConfig.id
            if (idCount[idUnique] > 1) {

                pluginConfigs.findAll { plugin ->
                    plugin.id == idUnique
                }.each {
                    duplicatePlugin ->
                        if (!duplicatePlugin instanceof ResourceConfig) {
                            LOG.error "Plugin identifier $pluginConfig.id cannot be used more than once"
                            somePluginReportedErrors = true;
                            // decrement the counter so we don't report the error more than once
                            idCount[pluginConfig.id] = 1

                        }
                }
            }
        }

        // add GLOBAL resource definition for those plugins that don't provide it explicitly:
        addDefaultNeed("GLOBAL", "excl", "false")
        addDefaultNeed("GLOBAL", "h_vmem", "2g")
        addDefaultNeed("GLOBAL", "virtual_free", "4g")

        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "excl", "false")
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "h_vmem", "10g")
        addDefaultNeed("ALIGNMENT_POST_PROCESSING", "virtual_free", "12g")

        loaded = true;
    }
    /**
     * Create artifacts install requests for the plugin given as argument. Traverse the graph of resource
     * dependency and order resource artifact installation such that resources that must be installed before
     * others are so. The client is responsible for deleting the result file when it is no longer needed.
     * @param pluginConfig
     * @return null if the plugin does not require any artifacts, or a unique ile containing pb requests.
     */
    public File createPbRequestFile(PluginConfig pluginConfig) {
        LOG.debug("createPbRequestFile for " + pluginConfig?.id)
        BuildArtifactRequest requestBuilder = new BuildArtifactRequest(webServerHostname)
        def uniqueFile = File.createTempFile("artifacts-install-requests", ".pb");
        // Create a single .pb file containing all resources that the plugin requires:
        // Each .pb file will contain the artifacts needed by the resource, starting with the artifacts that the
        // resource requires (deep first search)
        pluginConfig?.requires.each {
            resource ->

                def resourceConfig = lookupResource(resource.id, resource.versionAtLeast, resource.versionExactly)
                writePbForResource(resourceConfig, requestBuilder)
        }
        if (!requestBuilder.isEmpty()) {
            requestBuilder.save(uniqueFile);
            LOG.debug(requestBuilder.toString());
            return uniqueFile

        } else {
            return null;
        }

    }
    /**
     * Write PB artifact requests for a resource, starting with the artifacts of the resources required by the argument
     * resource.
     * @param resourceConfig
     * @param requestBuilder
     */
    def writePbForResource(ResourceConfig resourceConfig, BuildArtifactRequest requestBuilder) {
        LOG.debug("writePbForResource for " + resourceConfig?.id + " visiting resource dependencies..")
        if (!resourceConfig.requires.isEmpty()) {
            // recursively generate PB requests for resources required by this resource.
            for (Resource prerequisite : resourceConfig.requires) {
                ResourceConfig preResourceConfig = lookupResource(prerequisite.id,
                        prerequisite.versionAtLeast,
                        prerequisite.versionExactly)
                writePbForResource(preResourceConfig, requestBuilder)
            }

        }
        LOG.debug("writePbForResource for " + resourceConfig?.id + " writing artifact requests.")

        if (!resourceConfig.artifacts.isEmpty()) {
            // resource has artifacts. Generate the "install-requests.pb" file to tell the cluster nodes
            // how to install each artifact:

            String scriptFilename = resourceConfig.files.find { f -> f.id == "INSTALL" }.localFilename

            for (Artifact artifactXml : resourceConfig.artifacts) {
                LOG.debug(String.format("PB request.add(%s:%s)", resourceConfig.id, artifactXml.id))
                requestBuilder.addArtifactWithList(resourceConfig.id, artifactXml.id, resourceConfig.version,
                        scriptFilename, Artifacts.RetentionPolicy.REMOVE_OLDEST, constructAvp(artifactXml)
                )
            }
        }
        LOG.debug("writePbForResource for " + resourceConfig?.id + " done.")

    }

    static List<Artifacts.AttributeValuePair> constructAvp(Artifact artifact) {

        return artifact.attributes.collect {

            it ->
                def builder = Artifacts.AttributeValuePair.newBuilder().setName(it.name)
                if (it.value) {
                    builder.setValue(it.value)
                }
                builder.build()
        }

    }
/**
 * Add a default value to each plugin when key is not defined for the scope.
 * @param executablePluginConfig
 * @param scope
 * @param key
 * @param defaultValue
 */
    void addDefaultNeed(String scope, String key, String defaultValue) {
        pluginConfigs.each { pluginConfig ->
            if (pluginConfig instanceof ExecutablePluginConfig) {
                def execPlugin = pluginConfig

                if (execPlugin.getRuntime().needs().findAll { need ->
                    need.scope == scope &&
                            need.key == key
                }.isEmpty()) {
                    // add default value when no value was defined for key in scope:

                    execPlugin.getRuntime().needs().add(new Need(scope, key, defaultValue))
                }
            }
        }
    }

    void checkPluginResourceRequirements() {
    }
/**
 * Returns a string that describes registered plugins.
 * @return human readable plugins description.
 */
    public String describePlugins() {
        MutableString buffer = new MutableString();
        for (PluginConfig config : pluginConfigs) {
            def description = []
            description << "num-rules: ${config.options.rules().size()}"
            if (config.hasProperty("runtime") && config.runtime?.needs) {
                description << "num-needs: ${config.runtime.needs.size()}"
            }
            buffer.append(String.format(" %s/%s (%s) %s\n", getHumanReadablePluginType(config), config.name, config.version, description.join(", ")));
        }

        return buffer.toString();
    }
    /**
     * Read plugin configuration from the specified path.
     * @param location path to load from.
     */
    public void readConfigurationFromLocation(String location, boolean scanningResources = false) {
        //printf "-------- Reading Configuration from Location %s %n", location

        File directory = new File(location);
        if (directory == null || directory.list() == null) {
            LOG.info(String.format("Scanned location %s but ignored since empty.", location));
            return;
        }
        for (String filename : directory.list()) {

            if (ignoreFilenames(filename)) {
                continue;
            }
            readPluginConfigFile(new File(directory, filename).getAbsolutePath(), scanningResources);
        }

        // now check resources requirements, and remove the plugins that cannot find their resources:
        def toRemove = []
        for (PluginConfig config : pluginConfigs) {

            def errors = new ArrayList<String>()
            errors = checkRequiredResources(config, errors)
            if (!errors.isEmpty()) {
                toRemove.add(config)
                errors.each { message ->
                    LOG.error("An error occured resolving a plugin resource requirement: ${message}")
                }
            }
        }
        pluginConfigs.removeAll(toRemove)

    }

    public boolean somePluginReportedErrors() {
        somePluginReportedErrors
    }

    private boolean ignoreFilenames(String filename) {
        def ignoreList = [
                "environment.sh",
                ".svn",
                ".DS_Store"
        ];
        for (String ignore : ignoreList) {
            if (ignore.equals(filename)) return true;
        }
        return false;
    }


    private void readPluginConfigFile(String pluginConfigFilePath, boolean scanningResources) {
        // printf "Reading plugin dir: error=%b %s %n",somePluginReportedErrors(), pluginDirectory

        LOG.info("Scanning location ${pluginConfigFilePath}");
        javax.xml.bind.util.ValidationEventCollector validationCollector
        org.campagnelab.gobyweb.plugins.xml.PluginConfig config
        java.util.List<java.lang.String> errors
        java.io.File fileToUnmarshal
        (config, validationCollector, errors, fileToUnmarshal) = parsePluginConfig(pluginConfigFilePath, this)
        if (config == null) {
            return
        }

        if (!validationCollector.hasEvents()) {
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
                config.setPluginDirectory(pluginConfigFilePath);

                addScriptFile(scanningResources, config)
                addInstallFile(scanningResources, config)
                addServerSidetools(scanningResources, config)

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
            }
            somePluginReportedErrors = true
        }
    }

    public void saveToXml(PluginConfig config, OutputStream os) {

        marshaller.marshal(config, os)
        os.flush()
    }

    public List parsePluginConfig(String pluginConfigFilePath, Plugins pluginSystem) {

        //If the message regarding validation to be customized
        ValidationEventCollector validationCollector = new ValidationEventCollector();
        unmarshaller.setEventHandler(validationCollector);
        PluginConfig config
        List<String> errors = new ArrayList<String>()
        final File fileToUnmarshal = new File(pluginConfigFilePath, "config.xml");
        if (!fileToUnmarshal.exists()) {
            config = null;
        }

        try {
            config = (PluginConfig) unmarshaller.unmarshal(fileToUnmarshal);
            if (config == null) {

                errors.add("Cannot find config.xml file in plugin directory " + fileToUnmarshal);
                LOG.error "Cannot find config.xml file in plugin directory " + fileToUnmarshal
            }
        } catch (JAXBException e) {
            // Errors will be reported below, no need to log them here
            if (pluginSystem != null) {
                pluginSystem.somePluginReportedErrors = true
            }

            errors.add(e.getMessage())
            LOG.error("JAXBException when unmarshaling a plugin", e)
        }
        [config, validationCollector, errors, fileToUnmarshal]
    }
    /** if scanning a resource with artifacts, and files does not contain install.sh,
     we define the default install.sh file.          */
    private void addInstallFile(boolean scanningResources, PluginConfig config) {
        if (config instanceof ResourceConfig) {
            ResourceConfig resource = config as ResourceConfig;
            if (scanningResources && !resource.artifacts.isEmpty() &&
                    resource.files.find({ pluginFile ->
                        "install.sh".equals(pluginFile.filename)
                    }) == null) {

                def SCRIPT_FILE = new PluginFile()
                SCRIPT_FILE.filename = "install.sh"
                SCRIPT_FILE.id = "INSTALL"
                config.files.add(SCRIPT_FILE)
            }
        }
    }
    /** Add dependency on GOBYWEB_SERVER_SIDE resource plugin on each non resource plugins.
     */
    private void addServerSidetools(boolean scanningResources, PluginConfig pluginConfig) {

        if (!(pluginConfig instanceof ResourceConfig)) {

            ResourceConfig resource = lookupResource("GOBYWEB_SERVER_SIDE", "2.0", null)
            assert resource != null: " The GOBYWEB_SERVER_SIDE plugin resource must exist";
            Resource resourceRef = new Resource()
            resourceRef.id = resource.id
            resourceRef.versionExactly = resource.version

            if (!pluginConfig.requires.contains(resourceRef)) {

                pluginConfig.requires.add(0, resourceRef);
            }
        } /*else {
            println("${pluginConfig.id} not a resource")
        }   */

    }

    private void addScriptFile(boolean scanningResources, PluginConfig config) {
        if (!scanningResources && config.files.find({ pluginFile ->
            "script.sh".equals(pluginFile.filename)
        }) == null) {

            // if files do not contain script.sh, we define the default script file. (not for resources, since
            // don't have scripts.sh by default:
            def SCRIPT_FILE = new PluginFile()
            SCRIPT_FILE.filename = "script.sh"
            SCRIPT_FILE.id = "SCRIPT"
            config.files.add(SCRIPT_FILE)
        }
    }

/**
 * Check that pluginConfig's resources are available in this instance of GobyWeb.
 * @param pluginConfig
 * @param errors
 * @return
 */
    ArrayList<String> checkRequiredResources(PluginConfig pluginConfig, ArrayList<String> errors) {
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
    Map<String, String> pluginVersionsMap(PluginConfig pluginConfig) {
        pluginVersionsMap(pluginConfig, new LinkedHashMap<String, String>())
    }
    /**
     * Given a plugin, make a map of the plugin and resource names, to their version numbers.
     * @param pluginConfig the plugin to collect version numbers for
     * @return map of plugins to version numbers
     */
    Map<String, String> pluginVersionsMap(PluginConfig pluginConfig, Map<String, String> versionsMap) {

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

    private String webServerHostname;
    private ArrayList<String> serverConfDirectories = new ArrayList<String>();

    public void setWebServerHostname(String webServerHostname) {
        this.webServerHostname = webServerHostname;
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
            LOG.warn("serverConf does not exist or is not a directory: " + serverConfDirectory)
        }
    }

    private void locateSchema() {
        if (schemaFile == null) {
            for (String confDir in serverConfDirectories) {
                final File currentSchemaFile = new File("${confDir}/schemas/plugins.xsd")
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

    private JAXBContext jaxbContext = null;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    def installSchema(File schemaFile) {
        jaxbContext = JAXBContext.newInstance(PluginConfig.class);
        Schema schema;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // def url = new URL("http://campagnelab.org/schemas/gobyweb/plugins/plugins.xsd")

        if (schemaFile?.exists()) {
            schema = schemaFactory.newSchema(schemaFile);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();

            marshaller.setSchema(schema);
            unmarshaller.setSchema(schema);
        }
    }
/**
 * Returns the PluginConfig matching id or null if the plugin was not found.
 * @param idToFind
 * @return the PluginConfig that matches or null
 * @see Plugins#findPluginTypeById(java.lang.Class, java.lang.String)
 */
    public PluginConfig findById(String idToFind) {
        if (idToFind) {
            for (PluginConfig plugin in pluginConfigs) {
                if (plugin.id == idToFind) {
                    return plugin
                }
            }
        }
        return null;
    }

    /**
     * Returns the PluginConfig matching dbLegacyId or null if the plugin was not found.
     * @param type the type of Config object to find as it is possible dbLegacyId's can clash across types
     * @param idToFind the id to find
     * @return the matching PluginConfig or null
     */
    public PluginConfig findByDbLegacyId(Class type, String idToFind) {
        if (idToFind) {
            for (PluginConfig plugin in pluginConfigs) {
                if ((type.isAssignableFrom(plugin.getClass())) && (plugin.dbLegacyId && plugin.dbLegacyId == idToFind)) {
                    return plugin
                }
            }
        }
        return null;
    }

/**
 * Returns an aligner plugin config matching id or dbLegacyId or null if the plugin was not defined.
 * @param alignerId
 * @return
 * @see Plugins#findPluginTypeById(java.lang.Class, java.lang.String)
 */
    public AlignerConfig findAlignerById(String idToFind) {

        return (AlignerConfig) findPluginTypeById(AlignerConfig.class, idToFind);
    }
/**
 * Returns an executable plugin config matching id or dbLegacyId or null if the plugin was not defined.
 * @param alignerId
 * @return
 * @see Plugins#findPluginTypeById(java.lang.Class, java.lang.String)
 */
    public ExecutablePluginConfig findExecutableById(String idToFind) {

        return (ExecutablePluginConfig) findPluginTypeById(ExecutablePluginConfig.class, idToFind);
    }
/**
 * Returns an alignment analysis plugin config matching id or dbLegacyId or null if the plugin was not defined.
 * @param alignerId
 * @return
 * @see Plugins#findPluginTypeById(java.lang.Class, java.lang.String)
 */
    public AlignmentAnalysisConfig findAlignmentAnalysisById(String idToFind) {

        return (AlignmentAnalysisConfig) findPluginTypeById(AlignmentAnalysisConfig.class, idToFind);
    }
/**
 * Returns an plugin config of the specified type, matching id. The parameter idToFind may be null, in which
 * case null is returned.
 * @param idToFind PluginConfig identifier or dbLegacyId.
 * @param type Type of plugin config that should be returned (either AlignerConfig.class, AlignmentAnalysisConfig.class, ResourceConfig.class)
 * @return an instance of PluginConfig or null.
 */
    public PluginConfig findPluginTypeById(Class type, String idToFind) {
        if (idToFind) {
            for (PluginConfig plugin in pluginConfigs) {
                if ((type.isAssignableFrom(plugin.getClass())) && (plugin.id == idToFind)) {
                    return plugin
                }
            }
        }
        return null;
    }

    public String getHumanReadablePluginType(PluginConfig config) {
        def name = config.class.getSimpleName()
        name = name.replaceAll("Config\$", "")
        // insert underscores before upper-case characters, then uppercase the result. This adds _ as first character,
        // we remove it with substring below.
        name = name.replaceAll("[A-Z]", "_\$0").toUpperCase();
        name.substring(1)
    }

/**
 * Write a bash shell script with environment variable definitions for each automatic plugin option.
 * @param pluginConfig The plugin for which user-defined option values should be written.
 * @return The temporary file where options have been written.
 */
    public File generateAutoOptionsFile(ExecutablePluginConfig pluginConfig, String attributesPrefix = null, Map<String, String> attributes = null) {
        // call validate to force the update of user-defined values from default values.
        pluginConfig.validate()
        File autoOptionTmpFile = new File("/tmp/auto-options-${ICBStringUtils.generateRandomString(15)}.sh")

        PrintWriter writer = new PrintWriter(autoOptionTmpFile);

        List<Option> optionsToProcess =
            attributes == null ? pluginConfig.userSpecifiedOptions() : pluginConfig.options()
        for (Option option : optionsToProcess) {

            def optionValue
            if (attributes == null) {
                optionValue = option.userDefinedValue
            } else {
                def attributeName = (attributesPrefix ?: "") + option.id
                optionValue = attributes[attributeName] ?: ""
            }

            if (option.type == Option.OptionType.CATEGORY) {
                optionValue = option.categoryIdToValue(optionValue)
            }

            def pluginId = scriptImportedFrom(pluginConfig)
            if (pluginId != null) {
                PluginConfig fromPlugin = findExecutableById(pluginId);
                // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID, where plugin refers to the plugin we imported the script from:
                writer.println("PLUGINS_${getHumanReadablePluginType(fromPlugin)}_${fromPlugin.id}_${option.id}=\"${optionValue}\"")
            } else {
                // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID
                writer.println("PLUGINS_${getHumanReadablePluginType(pluginConfig)}_${pluginConfig.id}_${option.id}=\"${optionValue}\"")
            }
        }
        writeAutoFormatString(pluginConfig, writer, attributesPrefix, attributes);

        writer.println("# The plugin defines these files: ")
        for (PluginFile file : pluginConfig.files) {
            // write options in the format  ${PLUGINS_ TYPE _ plugin-id _ FILES _ file-id}
            writer.println("PLUGINS_${getHumanReadablePluginType(pluginConfig)}_${pluginConfig.id}_FILES_${file.id}=\${JOB_DIR}/${file.filename}")
        }


        writer.println("# The plugin has access to the following resources: ")
        for (Resource resourceRef : pluginConfig.requires) {
            writeResourceFileVariables(resourceRef, writer)
        }
        writer.flush()
        return autoOptionTmpFile;
    }

    private void writeResourceFileVariables(Resource resourceRef, PrintWriter writer) {

        // write variables for resource's requirements:
        ResourceConfig resource = lookupResource(resourceRef.id, resourceRef.versionAtLeast, resourceRef.versionExactly)
        for (Resource prerequisite : resource.requires) {
            writeResourceFileVariables(prerequisite, writer)
        }

        if (resource != null) {
            // write resources in the format  ${ RESOURCES _ resource-id _ file-id}
            for (PluginFile file : resource.files) {

                writer.println("RESOURCES_${resource.id}_${file.id}=\${JOB_DIR}/${file.filename}")
            }
        }
    }
/**
 * Write autoFormat options that are defined to the _ALL_OTHER_OPTIONS variable.
 * @param pluginConfig
 * @param writer to the auto-options.sh file.
 */
    void writeAutoFormatString(PluginConfig pluginConfig, PrintWriter writer, String attributesPrefix, Map<String, String> attributes) {
        // source plugin id -> string buffer map. String buffer will hold the AUTOFORMAT definition for the plugin
        Object2ObjectMap<String, StringBuffer> map = new Object2ObjectArrayMap<String, StringBuffer>()
        def sb

        List<Option> optionsToProcess =
            attributes == null ? pluginConfig.userSpecifiedOptions() : pluginConfig.options()
        for (Option option : optionsToProcess) {

            def optionValue
            if (attributes == null) {
                optionValue = option.userDefinedValue
            } else {
                def attributeName = (attributesPrefix ?: "") + option.id
                optionValue = attributes[attributeName]
            }
            if (option.type == Option.OptionType.CATEGORY) {
                optionValue = option.categoryIdToValue(optionValue)
            }

            def pluginId = scriptImportedFrom(pluginConfig)
            if (pluginId != null) {
                PluginConfig fromPlugin = findById(pluginId);
            }
            PluginConfig sourcePlugin;
            if (pluginId != null) {
                PluginConfig fromPlugin = findById(pluginId);
                sourcePlugin = fromPlugin
            } else {
                sourcePlugin = pluginConfig

            }
            def buffer = map.get(sourcePlugin.id)
            if (buffer == null) {
                map[sourcePlugin.id] = new StringBuffer(" ")
            }

            sb = map[sourcePlugin.id]
            // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID, where plugin refers to the plugin we imported the script from:
            if (option.autoFormat && optionValue != null) {

                switch (option.type) {
                    case Option.OptionType.BOOLEAN:
                        sb.append(String.format(option.flagFormat, optionValue));
                        break;
                    case Option.OptionType.SWITCH:
                        if (optionValue == "true") {
                            sb.append(String.format(option.flagFormat, optionValue))
                        };
                        break;
                    case Option.OptionType.STRING:
                    case Option.OptionType.INTEGER:
                    case Option.OptionType.DOUBLE:
                    case Option.OptionType.CATEGORY:
                        try {
                            sb.append(String.format(option.flagFormat, optionValue))
                        } catch (IllegalFormatConversionException e) {
                            LOG.error(String.format("plugin %s was unable to autoformat option %s to string: " + optionValue, pluginId,
                                    option.id), e);
                        }
                        break;
                }
                if (option.includeSpaces) {
                    sb.append(" ")
                }

            }
        }
        for (String pluginIdentifier : map.keySet()) {
            PluginConfig sourcePlugin = findById(pluginIdentifier)
            sb = map.get(pluginIdentifier)
            // write _ALL_OTHER_OPTIONS in the format PLUGINS _ TYPE _ PLUGIN-ID _ _ALL_OTHER_OPTIONS
            writer.println("PLUGINS_${getHumanReadablePluginType(sourcePlugin)}_${sourcePlugin.id}_ALL_OTHER_OPTIONS=\"${sb.toString()}\"")
        }
    }

    /**
     * Return the resource with largest version number, such that the resource has the identifier and at least the specified
     * version number.
     * @param resourceId
     * @param v required version
     * @return Most recent resource (by version number) with id and version>v
     */
    ResourceConfig lookupResource(String resourceId, String versionAtLeast, String versionExactly) {
        ArrayList<ResourceConfig> resourceList = (ArrayList<ResourceConfig>) pluginConfigs.findAll { resource ->
            if (versionExactly != null) {
                resource instanceof ResourceConfig &&
                        resource.id == resourceId &&
                        resource.exactlyVersion(versionExactly)
            } else if (versionAtLeast != null) {
                resource instanceof ResourceConfig &&
                        resource.id == resourceId &&
                        resource.atLeastVersion(versionAtLeast)
            }
        }
        resourceList = resourceList.sort { a, b -> (a.atLeastVersion(b.version) ? -1 : +1) }
        (ResourceConfig) resourceList[0]
    }
/**
 * Return the id of the plugin this plugin's script is imported from, or null if script was not imported.
 * @param config A plugin configuration.
 * @return Id of a plugin or null.
 */
    public String scriptImportedFrom(PluginConfig config) {
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
    public Map<String, Map<String, Object>> javaScriptMap(Class typeOfPlugin) {
        Map<String, HashMap<String, Object>> result = new HashMap<String, HashMap<String, Object>>();
        Object2BooleanOpenHashMap includeOptionInMap = new Object2BooleanOpenHashMap()

        def pluginsOfType = pluginConfigs.findAll { pluginConfig ->

            typeOfPlugin.isAssignableFrom(pluginConfig.getClass())
        };
        // determine which options should be listed in the map. We include options with a hiddenWhen attribute and options
        // these attributes reference.
        pluginsOfType.each {
            pluginConfig ->
                pluginConfig.options().each { option ->
                    if (option.hiddenWhenParsed != null) {
                        def fullId = pluginConfig.id + "_" + option.id
                        includeOptionInMap.put(fullId, true)
                        option.hiddenWhenParsed.get().optionIdsList().each {
                            optionIdReferenced ->

                                includeOptionInMap.put(pluginConfig.id + "_" + optionIdReferenced, true)
                        }
                    }
                }

        }
        pluginsOfType.each { pluginConfig ->
            pluginConfig.options().each { option ->
                def fullId = pluginConfig.id + "_" + option.id
                if (includeOptionInMap.get(fullId)) {

                    result.put(fullId, getJavaScriptOptionMap(pluginConfig, option))
                }
            }

        }
        result
    }

    public Map<String, Object> getJavaScriptOptionMap(PluginConfig plugin, Option option) {
        HashMap<String, Object> result = new HashMap<String, Object>();
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
            final PluginConfig pluginConfig,
            final org.campagnelab.gobyweb.plugins.xml.Script script,
            final Object gobywebObj,
            final Map bindings) {
        final File tempDir = null
        if (script.language == "groovy") {
            final File pluginScriptFilename = new File(pluginConfig.pluginDirectory, script.filename)
            if (pluginScriptFilename.exists()) {
                tempDir = Files.createTempDir()
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
}
