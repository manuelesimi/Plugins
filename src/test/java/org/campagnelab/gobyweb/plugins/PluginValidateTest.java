package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.Category;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.Option;
import org.campagnelab.gobyweb.plugins.xml.executables.Script;
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * @author campagne
 *         Date: 10/12/11
 *         Time: 11:18 AM
 */
public class PluginValidateTest {
    Plugins plugins;
    PluginRegistry pluginRegistry = PluginRegistry.getRegistry();

    @Test
    public void configure() {

        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf( "test-data/plugin-root-validate");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
        assertFalse(plugins.somePluginReportedErrors());
    }






}
