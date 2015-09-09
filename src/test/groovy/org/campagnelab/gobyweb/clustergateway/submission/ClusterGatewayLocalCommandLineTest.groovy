package org.campagnelab.gobyweb.clustergateway.submission

import org.apache.commons.io.FileUtils
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertTrue

/**
 * Tests the command line with a local artifact server.
 */
@RunWith(JUnit4.class)
class ClusterGatewayLocalCommandLineTest {

    static final String gatewayPluginRoot = "test-data/root-for-resource-installation";
    static final String envScript = "test-data/root-for-resource-installation/artifacts-config/env.sh";

    static final String resultsDir = String.format("test-results/gateway-local-command-line");
    static final String owner = "junit";
    static def repoDirAbsolutePath=new File("${resultsDir}/REPO").getAbsolutePath()


    @BeforeClass
    public static void configure() throws IOException {
        FileUtils.deleteDirectory(new File(resultsDir));
        FileUtils.forceMkdir(new File(resultsDir));
        FileUtils.forceMkdir(new File(resultsDir + "/artifacts"));

    }


    @Test
    public void installLocalResourceWithArtifacts() {
        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--resource VCF_TOOLS")));
        assertTrue(new File(repoDirAbsolutePath + "/artifacts/VCF_TOOLS/BINARIES/0.1.10").exists())
    }

    @Test
    public void installLocalResourceNoArtifacts() {
        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--resource PLAST")));
    }


    @Test
    public void installLocalResourceWithVersionNoArtifacts() {
        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--resource MERCURY:1.0")));
    }

    @Test
    public void installLocalResourceWithArtifactsAndAttributes() {
        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--attribute-value INDEX.organism=human --attribute-value INDEX.reference-build=1 --attribute-value INDEX.ensembl-version-number=74 --resource BWA_WITH_GOBY_ARTIFACT")));
        //assertTrue(new File(repoDirAbsolutePath + "/artifacts/VCF_TOOLS/BINARIES/0.1.10").exists())
    }

    private static String[] buildClusterGatewayArgs(String additionalCommands, String pluginRoot=gatewayPluginRoot,
                                                    boolean addHost=true) {
        ("--job-area ${new File(resultsDir).getAbsolutePath()}/GOBYWEB_SGE_JOBS " +
                "--fileset-area ${new File(resultsDir).getAbsolutePath()}/filesets " +
                "--plugins-dir ${pluginRoot} " +
                "--owner ${owner} " +
                "--env-script ${envScript} "+
                "--repository ${repoDirAbsolutePath} "+
                additionalCommands).split(" ");

    }
}
