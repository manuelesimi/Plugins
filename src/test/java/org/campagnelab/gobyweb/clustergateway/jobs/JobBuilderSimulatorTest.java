package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.clustergateway.jobs.simulator.JobBuilderSimulator;
import org.campagnelab.gobyweb.clustergateway.jobs.simulator.Option;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.SortedSet;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;

/**
 * Tester for {@link org.campagnelab.gobyweb.clustergateway.jobs.simulator.JobBuilderSimulator}.
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class JobBuilderSimulatorTest {

    static Plugins plugins;

    static AlignerConfig alignerConfig;

    static ResourceConfig resourceConfig;

    @Before
    public void configure() throws Exception {
        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-aligners");
        plugins.setWebServerHostname("localhost");
        plugins.reload();

        alignerConfig = plugins.getRegistry().findByTypedId("BWA_GOBY_ARTIFACT", AlignerConfig.class);
        assertNotNull(alignerConfig);

        resourceConfig = plugins.getRegistry().findByTypedId("BWA_WITH_GOBY_ARTIFACT", ResourceConfig.class);
        assertNotNull(resourceConfig);

    }

    @Test
    public void testJobSimulateAutoOptions() throws Exception {
        JobBuilderSimulator builderSimulator = new JobBuilderSimulator(alignerConfig,plugins.getRegistry());
        SortedSet<Option> autoOptions = builderSimulator.simulateAutoOptions();
        assertNotNull(autoOptions);
        assertNotSame("No auto-options generated",0,autoOptions.size());
    }

    @Test
    public void testResourceSimulateAutoOptions() throws Exception {
        JobBuilderSimulator builderSimulator = new JobBuilderSimulator(resourceConfig,plugins.getRegistry());
        SortedSet<Option> autoOptions = builderSimulator.simulateAutoOptions();
        assertNotNull(autoOptions);
        assertNotSame("No auto-options generated",0,autoOptions.size());
    }
}
