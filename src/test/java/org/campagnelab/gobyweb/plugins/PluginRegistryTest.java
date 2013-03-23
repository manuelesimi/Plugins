package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.Config;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Created with IntelliJ IDEA.
 * User: manuelesimi
 * Date: 2/9/13
 * Time: 2:11 PM
 */
@RunWith(JUnit4.class)
public class PluginRegistryTest {
    // TODO This test is not thread safe because it relies on the Registry, which uses a singleton internally.
    // Other tests started by Junit will alter the registry. Result will vary depending on which tests run first..
    static PluginRegistry registry = PluginRegistry.getARegistry();

    @BeforeClass
    public static void add() {
        AlignerConfig config = new AlignerConfig();
        config.setId("aligner_1");
        config.setName("aligner 1 goby output");
        config.setVersion("1.0");
        config.setHelp("Some help text.");
        registry.add(config);
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setId("resource_1");
        resourceConfig.setName("resource 1 goby ");
        resourceConfig.setVersion("1.1");
        resourceConfig.setHelp("Some help text.");
        registry.add(resourceConfig);
        AlignmentAnalysisConfig alignmentAnalysisConfig = new AlignmentAnalysisConfig();
        alignmentAnalysisConfig.setId("de");
        alignmentAnalysisConfig.setName("de 1 goby output");
        alignmentAnalysisConfig.setVersion("1.1");
        alignmentAnalysisConfig.setHelp("Some help text.");
        registry.add(alignmentAnalysisConfig);

    }

    @Test
    public void getResources() {
        List<ResourceConfig> resourceConfigs = registry.filterConfigs(ResourceConfig.class);
        assertEquals("There must be only one ResourceConfig", 1, resourceConfigs.size());
        assertEquals("ResourceConfigs not found", "resource_1", resourceConfigs.get(0).getId());

    }
    @Test
    public void getAligners() {
        List<AlignerConfig> alignerConfigs = registry.filterConfigs(AlignerConfig.class);
        assertEquals("There must be only one Aligner", 1, alignerConfigs.size());
        assertEquals("AlignerConfig not found", "aligner_1", alignerConfigs.get(0).getId());

    }
    @Test
    public void getAlignmentAnalyses() {
        List<AlignmentAnalysisConfig> alignmentAnalysisConfigs = registry.filterConfigs(AlignmentAnalysisConfig.class);
        assertEquals("There must be only one AlignmentAnalysisConfig", 1, alignmentAnalysisConfigs.size());
        assertEquals("AlignmentAnalysisConfig not found", "de", alignmentAnalysisConfigs.get(0).getId());

    }

    @Test
    public void getExecutables() {
        List<ExecutableConfig> executableConfigs = registry.filterConfigs(ExecutableConfig .class);
        assertEquals("There must be 2 ExecutableConfigs", 2, executableConfigs.size());
    }

    @Test
    public void findById() {
        assertNotNull("No aligner with id=aligner_1 found",registry.findById("aligner_1"));
    }


    @Test
    public void findByTypedId() {
        assertNotNull("Aligner with id=aligner_1 found",registry.findByTypedId("aligner_1", AlignerConfig.class));
        assertNull("Aligner with id=aligner_1 found, but it is an AlignmentAnalysisConfig", registry.findByTypedId("aligner_1", AlignmentAnalysisConfig.class));
        assertNotNull("Executable with id=aligner_1 found",registry.findByTypedId("aligner_1", ExecutableConfig.class));

    }

    @Test
    public void printAll() {
        for (Config conf : registry) {
            System.out.println(conf);
        }

    }
}
