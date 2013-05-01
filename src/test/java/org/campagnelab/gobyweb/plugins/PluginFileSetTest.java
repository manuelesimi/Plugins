package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.junit.Before;
import org.junit.Test;

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
