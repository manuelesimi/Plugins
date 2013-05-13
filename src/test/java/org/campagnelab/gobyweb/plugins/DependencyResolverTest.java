package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Test dependency resolution among plugins
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class DependencyResolverTest {

    Plugins plugins;

    @Before
    public void configure() {

        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-rnaselect");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }

    @Test
    public void resolveResourceMostRecent() {
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", null, null);
        assertNotNull(resourceConfig);
        assertEquals("2011.11.17", resourceConfig.getVersion());
    }

    @Test
    public void resolveResourceExactVersion() {
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", null, "2011.11.17");
        assertNotNull(resourceConfig);
        assertEquals("2011.11.17", resourceConfig.getVersion());
    }

    @Test
    public void resolveResourceAtLeastVersion() {
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", "2011.07.07", null);
        assertNotNull(resourceConfig);
        assertEquals("2011.11.17", resourceConfig.getVersion());
    }

    @Test
    public void resolveResourceAtMostVersion() {
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", null, null, "2011.11.17");
        assertNotNull(resourceConfig);
        assertEquals("2011.11.17", resourceConfig.getVersion());
    }

    @Test
    public void resolveResourceInvalidVersion() {
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", "2012.07.07", null);
        assertNull(resourceConfig);
    }

    @Test
    public void resolveFileSetExactVersion() {
        FileSetConfig fileSetConfig = DependencyResolver.resolveFileSet("COMPACT_READS", null, "1.0");
        assertNotNull(fileSetConfig);
        assertEquals("1.0", fileSetConfig.getVersion());
    }

    @Test
    public void resolveFileSetAtLeastVersion() {
        FileSetConfig fileSetConfig = DependencyResolver.resolveFileSet("COMPACT_READS", "1.0", null);
        assertNotNull(fileSetConfig);
        assertEquals("1.0", fileSetConfig.getVersion());
    }

    @Test
    public void resolveFileSetAtMostVersion() {
        FileSetConfig fileSetConfig = DependencyResolver.resolveFileSet("COMPACT_READS", null, null, "2.0");
        assertNotNull(fileSetConfig);
        assertEquals("1.0", fileSetConfig.getVersion());
    }

    @Test
    public void failToResolveFileSetAtLeastVersion() {
        FileSetConfig fileSetConfig = DependencyResolver.resolveFileSet("COMPACT_READS", "2.0", null);
        assertNull(fileSetConfig);
    }

}
