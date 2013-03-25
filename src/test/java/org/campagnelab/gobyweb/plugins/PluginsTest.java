package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.common.*;
import org.campagnelab.gobyweb.plugins.xml.executables.Category;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.Option;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
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
import java.util.List;
import java.util.Iterator;

import static junit.framework.Assert.*;

/**
 * @author campagne
 *         Date: 10/12/11
 *         Time: 11:18 AM
 */
public class PluginsTest {
    Plugins plugins;
    PluginRegistry pluginRegistry = PluginRegistry.getRegistry();

    @Before
    public void configure() {

        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf( "test-data/plugin-root-1");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }

    @Test
    public void loadConfig() {
        assertTrue("some plugins must be found", pluginRegistry.size() > 0);
    }

    @Test
    public void loadAlignerConfigs() {
        List<AlignerConfig> aligners = pluginRegistry.filterConfigs(AlignerConfig.class);
        assertTrue("some aligners must be found", aligners.size() > 0);
    }

    @Test
    public void loadAnalysisConfigs() {
        List<AlignmentAnalysisConfig> analyses = pluginRegistry.filterConfigs(AlignmentAnalysisConfig.class);
        assertTrue("some analyses must be found", analyses.size() > 0);
    }

    @Test
    public void loadResourceConfigs() {
        List<ResourceConfig> resources = pluginRegistry.filterConfigs(ResourceConfig.class);
        assertTrue("some resources must be found", resources.size() > 0);
    }

    @Test
    public void loadConfigByTypedId() {
        assertNotNull("GSNAP_GOBY not found",plugins.getRegistry().findByTypedId("GSNAP_GOBY", ExecutableConfig.class));
    }

    @Test
    public void deriveHumanReadableType() {
        assertEquals("ALIGNMENT_ANALYSIS", new AlignmentAnalysisConfig().getHumanReadableConfigType());
        assertEquals("ALIGNER", new AlignerConfig().getHumanReadableConfigType());
    }

    @Test
    public void testAutoOptionsAligner() {
        AlignerConfig config = new AlignerConfig();
        config.setId("aligner_1");
        config.setName("aligner 1 goby output");
        config.setHelp( "Some help text.");
        config.supportsBAMAlignments = false;
        config.supportsColorSpace = true;
        config.supportsFastaReads = true;
        config.supportsFastqReads = true;
        config.supportsGobyReads = true;
        config.supportsGobyAlignments = true;
        config.supportsBisulfiteConvertedReads = false;

        Option option = new Option();
        option.required = true;
        option.autoFormat = true;
        option.flagFormat = " --align-type=%s "; //GSNAP type format
        option.categories.add(new Category("cmet", "bisulfite alignment", "Instructs GSNAP to align bisulfite converted reads."));
        option.categories.add(new Category("id", "name", "hep."));
        option.defaultsTo = "DEFAULT_VALUE";
        option.help = "Help text";
        option.id = "align_type";
        option.name = "Alignment type";

        config.options.items().add(option);

        assertEquals("config must have no user-defined option", 0, config.userSpecifiedOptions().size());
        option.userDefinedValue = "DEFINED!";
        assertEquals("config must exactly one user-defined option", 1, config.userSpecifiedOptions().size());
        option.userDefinedValue = null;
        assertEquals("config must have no user-defined option", 0, config.userSpecifiedOptions().size());

        ArrayList<String> errors = new ArrayList<String>();
        config.validate(errors);
        assertEquals("config must exactly one user-defined option", 1, config.userSpecifiedOptions().size());

    }

    @Test
    public void testWriteArtifact() throws JAXBException {
        ResourceConfig config = new ResourceConfig();
        config.setId("aligner_1");
        config.setName("aligner 1 goby output");
        config.setHelp("Some help text.");

        final Artifact artifact = new Artifact();
        config.artifacts.add(artifact);
        final Attribute attribute = new Attribute();
        artifact.attributes.add(attribute);
        attribute.name = "attName";
        attribute.value = "attValue";
        JAXBContext context = JAXBContext.newInstance(ResourceConfig.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Write to System.out
        m.marshal(config, System.out);


    }

    @Test
    public void testAutoOptionsDiffExp() {
        AlignmentAnalysisConfig config = new AlignmentAnalysisConfig();
        config.setId("de");
        config.setName("aligner 1 goby output");
        config.setHelp("Some help text.");


        Option option = new Option();
        option.required = true;
        option.autoFormat = true;

        option.defaultsTo = "DEFAULT_VALUE";
        config.options.items().add(option);

        assertEquals("config must have no user-defined option", 0, config.userSpecifiedOptions().size());
        option.userDefinedValue = "DEFINED!";
        assertEquals("config must exactly one user-defined option", 1, config.userSpecifiedOptions().size());
        option.userDefinedValue = null;
        assertEquals("config must have no user-defined option", 0, config.userSpecifiedOptions().size());
        ArrayList<String> errors = new ArrayList<String>();
        config.validate(errors);
        assertEquals("config must exactly one user-defined option", 1, config.userSpecifiedOptions().size());


    }

    @Test
    public void atLeastVersion() {
        Resource ref = new Resource();
        ref.id = "id1";
        ref.versionAtLeast = "0.1.14";

        ResourceConfig resource = new ResourceConfig();
        resource.setVersion("0.1.14");
        assertTrue(resource.atLeastVersion("0.1.13"));
        assertTrue(resource.atLeastVersion("0.0.13"));
        assertTrue(resource.atLeastVersion("0.1.14"));
        assertFalse(resource.atLeastVersion("0.1.15"));
        assertFalse(resource.atLeastVersion("1"));


    }

    /**
     * SAMTOOLS/atLeast=0.1.17 cannot be satisfied as 0.1.14 is the highest version at the time of this test.
     */
    @Test
    public void loadResources() {
        assertNull(DependencyResolver.resolveResource("SAMTOOLS", null, "0.1.17"));
        assertNotNull(DependencyResolver.resolveResource("SAMTOOLS", "0.1.18", null));
    }


    /**
     * Load a resource with an artifact.
     */
    @Test
    public void loadResourcesWithArtifacts() {

        final ResourceConfig star = DependencyResolver.resolveResource("STAR", "2.2.0", "2.2.0");
        assertNotNull(star);
        assertFalse(star.artifacts.isEmpty());
        //
        assertEquals(1, star.files.size());
        final Iterator<PluginFile> iterator = star.files.iterator();
        PluginFile file = iterator.next();
        assertEquals("INSTALL", file.id);
        assertEquals("install.sh", file.filename);

    }

    /**
     * GSNAP_WITH_GOBY/exactly=2011.07.07 cannot be satisfied as it doesn't exist. but 2011.07.08 does.
     */
    @Test
    public void loadResourcesExactly() {
        assertNull(DependencyResolver.resolveResource("GSNAP_WITH_GOBY", null, "2011.07.07"));
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", null, "2011.07.08");
        assertNotNull(resourceConfig);
        assertEquals("2011.07.08", resourceConfig.getVersion());
    }


    @Test
    public void checkExecuteScript() {
        final AlignmentAnalysisConfig contaminant_extract = PluginRegistry.getRegistry().findByTypedId("CONTAMINANT_EXTRACT",AlignmentAnalysisConfig.class);
        assertNotNull(contaminant_extract);
        assertNotNull(contaminant_extract.execute);
        ArrayList<Script> executeScripts = contaminant_extract.execute.scripts();
        assertNotNull(executeScripts);
        assertTrue(!executeScripts.isEmpty());

    }

    /**
     * ++ This test is too fragile to commit as the highest version number will change periodically.
     * ++ After manually checking this when necessary, comment out the @Test annotation.
     * <p/>
     * GSNAP_WITH_GOBY/atLeast=2099.12.13 cannot be satisfied as it is too big a version.
     * 2011.01.01 can be satisfied and should return the HIGHEST version GSNAP_WITH_GOBY.
     */
    public void loadResourcesAtLeast() {
        assertNull(DependencyResolver.resolveResource("GSNAP_WITH_GOBY", "2099.12.31", null));
        ResourceConfig resourceConfig = DependencyResolver.resolveResource("GSNAP_WITH_GOBY", "2011.07.07", null);
        assertNotNull(resourceConfig);
        assertEquals("2011.11.17", resourceConfig.getVersion());
    }


    /**
     * Check that the same ID can be used to retrieve a resource, or an aligner, depending on the query method
     * used.
     */
    @Test
    public void idScopes() {
        AlignerConfig alignerById = PluginRegistry.getRegistry().findByTypedId("GSNAP_WITH_GOBY",AlignerConfig.class);
        assertNotNull(alignerById);
        assertNotNull(PluginRegistry.getRegistry().findByTypedId("GSNAP_WITH_GOBY", ResourceConfig.class));
        ResourceConfig bwa_goby_resource = PluginRegistry.getRegistry().findByTypedId("GSNAP_WITH_GOBY",ResourceConfig.class);
        assertNotNull(bwa_goby_resource);
        assertNotSame(alignerById, bwa_goby_resource);
    }


}
