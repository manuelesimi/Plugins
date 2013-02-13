package org.campagnelab.gobyweb.plugins.xml;

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

/**
 * List the configuration classes to register when creating a new instance of {@link javax.xml.bind.JAXBContext}
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
    ResourceConfig {
        public Class<? extends Config> register() {return ResourceConfig.class;}
    };

    public Class<? extends Config> register() {throw new UnsupportedOperationException();}
}
