package org.campagnelab.gobyweb.plugins.xml

import org.campagnelab.gobyweb.plugins.PluginRegistry
import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.*;

/**
 * Reference to a configuration.
 * @author Fabien Campagne
 * Date: 1/22/13
 * Time: 10:25 AM
 *
 */
class ConfigRef {

    PluginRegistry pluginRegistry = PluginRegistry.getRegistry();
    Class<? extends Config> type;
    String id;
    String version;

    static ConfigRef parseDescription(String pluginDescription) {
        String []tokens = pluginDescription.split(":")
        ConfigRef ref = new ConfigRef()
        int tokenIndex = 0;
        int mustHave = 2;
        switch (tokens[tokenIndex]) {
            case "ALIGNER":
                ref.type = CONFIGS_TO_CLASSES.AlignerConfig.register()
                mustHave = 2;
                break;
            case "ALIGNMENT_ANALYSIS":
                ref.type = CONFIGS_TO_CLASSES.AlignmentAnalysisConfig.register()
                mustHave = 2;
                break;
            case "RESOURCE":
                ref.type = CONFIGS_TO_CLASSES.ResourceConfig.register()
                mustHave = 3;
                break;
            default:
                // no type specifier:
                tokenIndex--;
                mustHave = 1
                ref.type = null;
        }
        if (tokens.length < mustHave) {
            if (mustHave == 2) {
                throw new IllegalArgumentException("plugin description must have at least two arguments: pluginId:version")
            }
            if (mustHave == 3) {
                throw new IllegalArgumentException("when specifying type, plugin description must have at least three arguments: type:pluginId:version")
            }
        }

        tokenIndex++;
        ref.id = tokens[tokenIndex].trim()
        if (ref.type.getClass().isAssignableFrom(CONFIGS_TO_CLASSES.ResourceConfig.register()))   {
            // resources have version numbers:
            ref.version = tokens[tokenIndex + 1]
        }
        ref
    }

    /** Find the config identified by this reference in the provided plugin system.
     *
     * @param plugins  Plugin system
     * @return  the pluginConfig corresponding to the reference.
     */
    Config instantiate() {
        if (type == null) {
           return pluginRegistry.findById(id)
        } else {
            return pluginRegistry.findByTypedId(id,type)
        }
    }

}
