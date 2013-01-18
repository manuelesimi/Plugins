package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.artifacts.ArtifactRequestHelper;
import org.campagnelab.gobyweb.artifacts.Artifacts;
import org.campagnelab.gobyweb.plugins.xml.*;
import org.junit.Before;
import org.junit.Test;

import javax.print.attribute.Size2DSyntax;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import static junit.framework.Assert.*;

/**
 * @author campagne
 *         Date: 10/12/11
 *         Time: 11:18 AM
 */
public class ResourceDependencyTest {
    Plugins plugins;

    @Before
    public void configure() {

        plugins = new Plugins();
        plugins.addServerConf("test-data/plugin-root-3");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
    }

    @Test
    public void noPbRequests() {
        AlignerConfig alignerById = plugins.findAlignerById("GSNAP_WITH_GOBY");
        File requests = plugins.createPbRequestFile(alignerById);
        assertNull("Aligner plugin GSNAP_WITH_GOBY has no artifact dependencies", requests);

    }


    @Test
    public void writePbRequests() throws IOException {

        PluginConfig starAligner = plugins.findAlignerById("STAR22_GOBY");
        assertNotNull("STAR aligner must be found", starAligner);
        File requests = plugins.createPbRequestFile(starAligner);
        assertNotNull("Aligner plugin STAR must have some artifact dependencies", requests);

        ArtifactRequestHelper helper = new ArtifactRequestHelper(requests);

        assertEquals("artifacts {\n" +
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
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"SAMTOOLS\"\n" +
                "  artifact_id: \"SAMTOOLS\"\n" +
                "  version: \"0.1.18.1\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  retention: REMOVE_OLDEST\n" +
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
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"GSNAP_WITH_GOBY\"\n" +
                "  artifact_id: \"GSNAP_WITH_GOBY\"\n" +
                "  version: \"2011.07.08\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  retention: REMOVE_OLDEST\n" +
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"STAR\"\n" +
                "  artifact_id: \"EXECUTABLE\"\n" +
                "  version: \"2.2.0\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  retention: REMOVE_OLDEST\n" +
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
                "}\n" +
                "artifacts {\n" +
                "  plugin_id: \"SAMTOOLS\"\n" +
                "  artifact_id: \"SAMTOOLS\"\n" +
                "  version: \"0.1.18.1\"\n" +
                "  script_install_path: \"INSTALL_PATH_OMITTED\"\n" +
                "  ssh_web_app_host: \"localhost\"\n" +
                "  retention: REMOVE_OLDEST\n" +
                "}\n", cleanup(helper.getRequests()).toString());
    }

    private Artifacts.InstallationSet cleanup(Artifacts.InstallationSet requests) {
   // remove installation path because this is subject to change from one development machine to the next.
        final Artifacts.InstallationSet.Builder builder = requests.newBuilderForType();
        for (Artifacts.ArtifactDetails request: requests.getArtifactsList()) {
         builder.addArtifacts(request.toBuilder().setScriptInstallPath("INSTALL_PATH_OMITTED").build());
        }
        return builder.build();
    }
}
