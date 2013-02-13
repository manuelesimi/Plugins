package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Test invalid plugins.  test-data/plugin-root-2 only contains invalid plugins and should not load anything.
 *
 * @author campagne
 *         Date: 10/12/11
 *         Time: 11:18 AM
 */
public class InvalidPluginsTest {
    Plugins plugins;
    PluginRegistry pluginRegistry = PluginRegistry.getRegistry();

    @Before
    public void configure() {
        plugins = new Plugins();
        plugins.addServerConf("./test-data/plugin-root-2");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }

    @Test
    public void loadConfig() {
        assertEquals("no valid plugins must be found", 0, pluginRegistry.filterConfigs(AlignerConfig.class).size());
        assertEquals("no valid plugins must be found", 0, pluginRegistry.filterConfigs(AlignmentAnalysisConfig.class).size());
    }


}
