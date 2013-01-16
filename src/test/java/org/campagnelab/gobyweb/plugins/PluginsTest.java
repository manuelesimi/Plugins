package org.campagnelab.gobyweb.plugins;

import org.apache.commons.io.FilenameUtils;
import org.campagnelab.gobyweb.artifacts.ArtifactRequestHelper;
import org.campagnelab.gobyweb.plugins.xml.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import static junit.framework.Assert.*;

/**
 * @author campagne
 *         Date: 10/12/11
 *         Time: 11:18 AM
 */
public class PluginsTest {
    Plugins plugins;

    @Before
    public void configure() {

        plugins = new Plugins();
        plugins.addServerConf( "test-data/plugin-root-1");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }

    @Test
    public void loadConfig() {
        assertTrue("some plugins must be found", plugins.getAlignerPluginConfigs().size() > 0);
    }

    @Test
    public void deriveHumanReadableType() {
        assertEquals("ALIGNMENT_ANALYSIS", plugins.getHumanReadablePluginType(new AlignmentAnalysisConfig()));
        assertEquals("ALIGNER", plugins.getHumanReadablePluginType(new AlignerConfig()));
    }

    @Test
    public void testAutoOptionsAligner() {
        AlignerConfig config = new AlignerConfig();
        config.id = "aligner_1";
        config.name = "aligner 1 goby output";
        config.help = "Some help text.";
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
        config.id = "aligner_1";
        config.name = "aligner 1 goby output";
        config.help = "Some help text.";

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
        config.id = "de";
        config.name = "aligner 1 goby output";
        config.help = "Some help text.";


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
        resource.version = "0.1.14";
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
        assertNull(plugins.lookupResource("SAMTOOLS", null, "0.1.17"));
        assertNotNull(plugins.lookupResource("SAMTOOLS", "0.1.18", null));
    }


    /**
     * Load a resource with an artifact.
     */
    @Test
    public void loadResourcesWithArtifacts() {

        final ResourceConfig star = plugins.lookupResource("STAR", "2.2.0", "2.2.0");
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
        assertNull(plugins.lookupResource("GSNAP_WITH_GOBY", null, "2011.07.07"));
        ResourceConfig resourceConfig = plugins.lookupResource("GSNAP_WITH_GOBY", null, "2011.07.08");
        assertNotNull(resourceConfig);
        assertEquals("2011.07.08", resourceConfig.version);
    }


    @Test
    public void checkExecuteScript() {
        final AlignmentAnalysisConfig contaminant_extract = plugins.findAlignmentAnalysisById("CONTAMINANT_EXTRACT");
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
    @Test
    public void loadResourcesAtLeast() {
        assertNull(plugins.lookupResource("GSNAP_WITH_GOBY", "2099.12.31", null));
        ResourceConfig resourceConfig = plugins.lookupResource("GSNAP_WITH_GOBY", "2011.07.07", null);
        assertNotNull(resourceConfig);
        assertEquals("2011.11.17", resourceConfig.version);
    }


    /**
     * Check that the same ID can be used to retrieve a resource, or an aligner, dependending on the query method
     * used.
     */
    @Test
    public void idScopes() {
        AlignerConfig alignerById = plugins.findAlignerById("GSNAP_WITH_GOBY");
        assertNotNull(alignerById);
        assertNotNull(plugins.findExecutableById("GSNAP_WITH_GOBY"));
        PluginConfig bwa_goby_resource = plugins.findPluginTypeById(ResourceConfig.class, "GSNAP_WITH_GOBY");
        assertNotNull(bwa_goby_resource);
        assertNotSame(alignerById, bwa_goby_resource);
    }


}
