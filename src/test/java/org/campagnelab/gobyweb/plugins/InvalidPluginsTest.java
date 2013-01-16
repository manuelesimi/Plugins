package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.util.ArrayList;
import java.util.Iterator;

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

    @Before
    public void configure() {
        plugins = new Plugins();
        plugins.addServerConf("./test-data/plugin-root-2");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }

    @Test
    public void loadConfig() {
        assertEquals("no valid plugins must be found", 0, plugins.getAlignerPluginConfigs().size());
        assertEquals("no valid plugins must be found", 0, plugins.getAlignmentAnalysisConfigs().size());
    }


}
