package org.campagnelab.gobyweb.plugins;

/**
 * Settings used during the configurations loading activities
 */
class PluginLoaderSettings {

    /**
     * the root XML Schema
     */
    protected static final String SCHEMA = "/schemas/plugins.xsd";

    /**
     * Directories under serverConfig that are scanned for configurations
     */
    protected static final String[] SCANNED_DIRS = new String[] {
            "/plugins/resources",   // note that we define resources first, before any plugin that may require them.
            "/plugins/aligners",
            "/plugins/analyses"
    };

    /**
     * List of files/dirs ignored by the loader when reading SCANNED_DIRS
     */
    protected static final String[] IGNORED_FILES = new String[] {
            "environment.sh",
            ".svn",
            ".DS_Store"
    };

    /**
     * Resource configuration added to each non resource configuration.
     */
    protected static final String[] SERVER_SIDE_TOOL = new String[] {
            "GOBYWEB_SERVER_SIDE", //ID
            "2.0",  //versionAtLeast
            null //versionExactly
    };
}
