package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.Category;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.Option;
import org.campagnelab.gobyweb.plugins.xml.executables.Script;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
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
public class PluginFileSetTest {
    Plugins plugins;
    PluginRegistry pluginRegistry = PluginRegistry.getRegistry();

    @Before
    public void configure() {

        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf( "test-data/root-for-rnaselect");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }


    @Test
    public void loadFileSetConfigs() {
        List<FileSetConfig> fileSets = pluginRegistry.filterConfigs(FileSetConfig.class);
        assertTrue("some fileSets must be found", fileSets.size() > 0);
    }

    @Test
    public void loadTaskConfigs() {
        List<TaskConfig> taskSets = pluginRegistry.filterConfigs(TaskConfig.class);
        assertTrue("some tasks must be found", taskSets.size() > 0);
    }



}
