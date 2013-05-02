package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.Config;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

/**
 * Settings used during the configuration loading activities
 * @author manuele
 */
public class PluginLoaderSettings {

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
            "/plugins/analyses",
            "/plugins/filesets", // fileSets must be loaded before tasks, as they may refer them
            "/plugins/tasks"
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

    /**
     * Fileset configuration configuration added to each AlignerConfig's InputSchema.
     */
    public static final String[] COMPACT_READS = new String[] {
            "COMPACT_READS", //ID
            "1.0",  //versionAtLeast
            null, //versionExactly
            null //versionAtMost
    };

    /**
     * Fileset configuration configuration added to each AlignerConfig's InputSchema.
     */
    public static final String[] GOBY_ALIGNMENTS = new String[] {
            "GOBY_ALIGNMENTS", //ID
            "1.0",  //versionAtLeast
            null, //versionExactly
            null //versionAtMost
    };


    /**
     * Fileset configuration configuration added to each AlignerConfig's InputSchema.
     */
    public static final String[] BAM_ALIGNMENTS = new String[] {
            "BAM_ALIGNMENTS", //ID
            "1.0",  //versionAtLeast
            null, //versionExactly
            null //versionAtMost
    };

    /**
     * List the configuration classes to register when a new instance of {@link javax.xml.bind.JAXBContext} is being created
     */
    public enum CONFIGS_TO_CLASSES {

        AlignerConfig {
            public Class<? extends Config> register() {return AlignerConfig.class;}
        },
        AlignmentAnalysisConfig {
            public Class<? extends Config> register() {return AlignmentAnalysisConfig.class;}
        },
        FileSetConfig {
            public Class<? extends Config> register() {return FileSetConfig.class;}
        },
        TaskConfig {
            public Class<? extends Config> register() {return TaskConfig.class;}
        },
        ResourceConfig {
            public Class<? extends Config> register() {return ResourceConfig.class;}
        };

        public Class<? extends Config> register() {throw new UnsupportedOperationException();}
    }
}
