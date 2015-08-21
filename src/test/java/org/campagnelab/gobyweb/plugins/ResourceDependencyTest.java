package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.artifacts.ArtifactRequestHelper;
import org.campagnelab.gobyweb.artifacts.Artifacts;
import org.campagnelab.gobyweb.plugins.xml.Config;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.junit.Before;
import org.junit.Test;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

/**
 * @author campagne
 *         Date: 10/12/11
 *         Time: 11:18 AM
 */
public class ResourceDependencyTest {
    Plugins plugins;
    PluginRegistry pluginRegistry = PluginRegistry.getRegistry();

    @Before
    public void configure() {

        plugins = new Plugins();
        plugins.addServerConf("test-data/plugin-root-3");
        plugins.setWebServerHostname("localhost");
        plugins.reload();

    }

    @Test
    public void printAll() {
        for (Config conf : pluginRegistry) {
            System.out.println(conf);
        }

    }

    @Test
    public void noPbRequests() {
        AlignerConfig alignerById = pluginRegistry.findByTypedId("GSNAP_WITH_GOBY", AlignerConfig.class);
        File requests = plugins.createPbRequestFile(alignerById);
        assertNull("Aligner plugin GSNAP_WITH_GOBY has no artifact dependencies", requests);

    }


    @Test
    public void writePbRequests() throws IOException {

        AlignerConfig starAligner = pluginRegistry.findByTypedId("STAR22_GOBY", AlignerConfig.class);
        assertNotNull("STAR aligner must be found", starAligner);
        File requests = plugins.createPbRequestFile(starAligner);
        assertNotNull("Aligner plugin STAR must have some artifact dependencies", requests);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(requests);

        assertEquals("artifacts {\n" +
                "  plugin_id: \"GROOVY\"\n"+
                "  artifact_id: \"DISTRIBUTION\"\n"+
                "  version: \"2.0.6\"\n"+
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n"+
                "  ssh_web_app_host: \"localhost\"\n"+
                "  retention: REMOVE_OLDEST\n"+
                "  mandatory: true\n"+
                "}\n"+
                "artifacts {\n"+
                "  plugin_id: \"ENSEMBL_GENOMES\"\n" +
                "  artifact_id: \"TOPLEVEL\"\n" +
                "  version: \"1.1\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  attributes {\n" +
                "    name: \"organism\"\n" +
                "  }\n" +
                "  attributes {\n" +
                "    name: \"reference-build\"\n" +
                "  }\n" +
                "  attributes {\n" +
                "    name: \"ensembl-version-number\"\n" +
                "  }\n" +
                "  retention: REMOVE_OLDEST\n" +
                "  mandatory: false\n" +
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"SAMTOOLS\"\n" +
                "  artifact_id: \"SAMTOOLS\"\n" +
                "  version: \"0.1.18.1\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  retention: REMOVE_OLDEST\n" +
                "  mandatory: false\n" +
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"FAI_INDEXED_GENOMES\"\n" +
                "  artifact_id: \"SAMTOOLS_FAI_INDEX\"\n" +
                "  version: \"1.1.1\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  attributes {\n" +
                "    name: \"organism\"\n" +
                "  }\n" +
                "  attributes {\n" +
                "    name: \"reference-build\"\n" +
                "  }\n" +
                "  attributes {\n" +
                "    name: \"ensembl-version-number\"\n" +
                "  }\n" +
                "  retention: REMOVE_OLDEST\n" +
                "  mandatory: false\n" +
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"STAR\"\n" +
                "  artifact_id: \"EXECUTABLE\"\n" +
                "  version: \"2.2.0\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  retention: REMOVE_OLDEST\n" +
                "  mandatory: false\n" +
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"STAR\"\n" +
                "  artifact_id: \"INDEX\"\n" +
                "  version: \"2.2.0\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  attributes {\n" +
                "    name: \"organism\"\n" +
                "  }\n" +
                "  attributes {\n" +
                "    name: \"reference-build\"\n" +
                "  }\n" +
                "  attributes {\n" +
                "    name: \"ensembl-version-number\"\n" +
                "  }\n" +
                "  retention: REMOVE_OLDEST\n" +
                "  mandatory: false\n" +
                "}\n"  , cleanup(helper.getRequests()).toString());
    }

    @Test
    public void testPluginVersionMap() {
        AlignerConfig starAligner = pluginRegistry.findByTypedId("STAR22_GOBY", AlignerConfig.class);

        Map<String, String> map = plugins.pluginVersionsMap(starAligner);
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
            buffer.append("\n");

        }
        System.out.println(buffer.toString());


        assertEquals("org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig:STAR22_GOBY:STAR 2.20 (Goby output)=1.2\n" +
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:MERCURY:Mercury messaging API=1.0\n" +
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:GOBYWEB_SERVER_SIDE:GobyWeb server side tools=2.6\n"+
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:GROOVY:Groovy language=2.0.6\n"+
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:FETCH_URL:Fetch and cache URL content=1.2\n"+
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:BASH_LIBRARY:Library of Functions for the Bash Shell=1.0\n"+
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:STAR:STAR=2.2.0\n" +
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:FAI_INDEXED_GENOMES:FAI indexed genomes=1.1.1\n" +
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:ENSEMBL_GENOMES:Ensembl Genomes=1.1\n" +
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:SAMTOOLS:Samtools=0.1.18.1\n" +
                "org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig:GSNAP_WITH_GOBY:GSNAP with Goby support=2011.07.08\n", buffer.toString());

    }

    private Artifacts.InstallationSet cleanup(Artifacts.InstallationSet requests) {
        // remove installation path because this is subject to change from one development machine to the next.
        final Artifacts.InstallationSet.Builder builder = requests.newBuilderForType();
        for (Artifacts.ArtifactDetails request : requests.getArtifactsList()) {
            builder.addArtifacts(request.toBuilder().setScriptInstallPath("INSTALL_PATH_OMITTED").build());
        }
        return builder.build();
    }
}
