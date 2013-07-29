package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;

/**
 * Tester for {@link JobBuilderSimulator}.
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class JobBuilderSimulatorTest {

    static Plugins plugins;

    static AlignerConfig alignerConfig;

    @Before
    public void configure() throws Exception {
        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-aligners");
        plugins.setWebServerHostname("localhost");
        plugins.reload();

        alignerConfig = plugins.getRegistry().findByTypedId("BWA_GOBY_ARTIFACT", AlignerConfig.class);
        assertNotNull(alignerConfig);
    }

    @Test
    public void testSimulateAutoOptions() throws Exception {
        JobBuilderSimulator builderSimulator = new JobBuilderSimulator(alignerConfig,plugins.getRegistry());
        Map<String,String> autoOptions = builderSimulator.simulateAutoOptions();
        assertNotNull(autoOptions);
        assertNotSame("No auto-options generated",0,autoOptions.size());
    }
}
