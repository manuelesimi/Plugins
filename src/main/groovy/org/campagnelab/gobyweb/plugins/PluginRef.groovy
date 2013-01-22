package org.campagnelab.gobyweb.plugins

import org.campagnelab.gobyweb.plugins.xml.PluginConfig

/**
 * Reference to a plugin.
 * @author Fabien Campagne
 * Date: 1/22/13
 * Time: 10:25 AM
 *
 */
class PluginRef {

    static PluginRef parseDescription(String pluginDescription) {
        String []tokens = pluginDescription.split(":")

        PluginRef ref = new PluginRef()
        ref.type = PluginType.UNDEFINED;
        int tokenIndex = 0;
        int mustHave = 2;
        switch (tokens[tokenIndex]) {
            case "ALIGNER":
                ref.type = PluginType.ALIGNER
                mustHave = 2;
                break;
            case "ALIGNMENT_ANALYSIS":
                ref.type = PluginType.ALIGNMENT_ANALYSIS
                mustHave = 2;
                break;
            case "RESOURCE":
                ref.type = PluginType.RESOURCE
                mustHave = 3;
                break;
            default:
                // no type specifier:
                tokenIndex--;
                mustHave = 1
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
        ref.id = tokens[tokenIndex]
        if (ref.type == PluginType.RESOURCE) {
            // resources have version numbers:
            ref.version = tokens[tokenIndex + 1]
        }
        ref
    }

    /** Find the plugin identified by this reference in the provided plugin system.
     *
     * @param plugins  Plugin system
     * @return  the pluginConfig corresponding to the reference.
     */
    PluginConfig instantiate(Plugins plugins) {
        PluginConfig pluginConfig = null;

        switch (type) {
            case PluginRef.PluginType.ALIGNMENT_ANALYSIS:
                pluginConfig = plugins.findAlignmentAnalysisById(id);
                break;
            case PluginRef.PluginType.ALIGNER: pluginConfig = plugins.findAlignerById(id);
                break;
            case PluginRef.PluginType.RESOURCE:
                pluginConfig = plugins.lookupResource(id, null, version);
                break;
            default:
                pluginConfig=plugins.findById(id)
        }
        return pluginConfig
    }

    public static enum PluginType {
        UNDEFINED, ALIGNER, ALIGNMENT_ANALYSIS, RESOURCE
    };

    PluginType type;

    String id;
    String version;
}
