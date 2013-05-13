package org.campagnelab.gobyweb.plugins

import edu.cornell.med.icb.util.ICBStringUtils
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.apache.log4j.Logger
import org.campagnelab.gobyweb.plugins.xml.Config
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig
import org.campagnelab.gobyweb.plugins.xml.executables.Option
import org.campagnelab.gobyweb.plugins.xml.resources.Resource
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig

/**
 *
 * @author Fabien Campagne
 * Date: 3/21/13
 * Time: 12:03 PM
 *
 */
class AutoOptionsFileHelper {

    private static Logger LOG = Logger.getLogger(Plugins.class);

    private PluginRegistry registry;

    AutoOptionsFileHelper(PluginRegistry registry) {
        this.registry = registry
    }
/**
 * Write a bash shell script with environment variable definitions for each automatic plugin option.
 * @param pluginConfig The plugin for which user-defined option values should be written.
 * @return The temporary file where options have been written.
 */
    public File generateAutoOptionsFile(ExecutableConfig pluginConfig, String attributesPrefix = null, Map<String, String> attributes = null) {
        // call validate to force the update of user-defined values from default values.
        pluginConfig.validate(new Vector<String>())
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

            def pluginId = Plugins.scriptImportedFrom(pluginConfig)
            if (pluginId != null) {
                ExecutableConfig fromPlugin = registry.findByTypedId(pluginId, ExecutableConfig.class);
                // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID, where plugin refers to the plugin we imported the script from:
                writer.println("PLUGINS_${fromPlugin.getHumanReadableConfigType()}_${fromPlugin.getId()}_${option.id}=\"${optionValue}\"")
            } else {
                // write options in the format PLUGINS _ TYPE _ PLUGIN-ID _ OPTION-ID
                writer.println("PLUGINS_${pluginConfig.getHumanReadableConfigType()}_${pluginConfig.id}_${option.id}=\"${optionValue}\"")
            }
        }
        writeAutoFormatString(pluginConfig, writer, attributesPrefix, attributes);

        writer.println("# The plugin defines these files: ")
        for (PluginFile file : pluginConfig.getFiles()) {
            // write options in the format  ${PLUGINS_ TYPE _ plugin-id _ FILES _ file-id}
            writer.println("PLUGINS_${pluginConfig.getHumanReadableConfigType()}_${pluginConfig.getId()}_FILES_${file.id}=\${JOB_DIR}/${file.filename}")
        }

        writer.println("# The plugin has access to the following resources: ")
        for (Resource resourceRef : pluginConfig.getRequiredResources()) {
            writeResourceFileVariables(resourceRef, writer)
        }
        writer.flush()
        return autoOptionTmpFile;
    }


    private void writeResourceFileVariables(Resource resourceRef, PrintWriter writer) {
        ResourceConfig resource = DependencyResolver.resolveResource(resourceRef.id, resourceRef.versionAtLeast, resourceRef.versionExactly)
        if (resource == null)
            return;
        // write variables for resource's requirements:
        for (Resource prerequisite : resource.requires) {
            writeResourceFileVariables(prerequisite, writer)
        }

        // write resources in the format  ${ RESOURCES _ resource-id _ file-id}
        for (PluginFile file : resource.files) {
            writer.println("RESOURCES_${resource.id}_${file.id}=\${JOB_DIR}/${file.filename}")
        }
    }
    /**
     * Writes autoFormat options that are defined to the _ALL_OTHER_OPTIONS variable.
     * @param pluginConfig
     * @param writer to the auto-options.sh file.
     */
    void writeAutoFormatString(ExecutableConfig executableConfig, PrintWriter writer, String attributesPrefix, Map<String, String> attributes) {
        // source plugin id -> string buffer map. String buffer will hold the AUTOFORMAT definition for the plugin
        Object2ObjectMap<String, StringBuffer> map = new Object2ObjectArrayMap<String, StringBuffer>()
        def sb

        List<Option> optionsToProcess =
            attributes == null ? executableConfig.userSpecifiedOptions() : executableConfig.options()
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

            def pluginId = Plugins.scriptImportedFrom(executableConfig)
            //what's that for?
            if (pluginId != null) {
                Config fromPlugin = registry.findById(pluginId);
            }
            Config sourcePlugin;
            if (pluginId != null) {
                sourcePlugin = registry.findById(pluginId);
            } else {
                sourcePlugin = executableConfig

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
            Config sourcePlugin = registry.findById(pluginIdentifier)
            sb = map.get(pluginIdentifier)
            // write _ALL_OTHER_OPTIONS in the format PLUGINS _ TYPE _ PLUGIN-ID _ _ALL_OTHER_OPTIONS
            writer.println("PLUGINS_${sourcePlugin.getHumanReadableConfigType()}_${sourcePlugin.id}_ALL_OTHER_OPTIONS=\"${sb.toString()}\"")
        }
    }
}
