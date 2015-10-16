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
        String attributes = "--attribute-value LAST_INDEX.TOPLEVEL_IDS.organism=HOMO_SAPIENS --attribute-value LAST_INDEX.TOPLEVEL_IDS.reference-build=1000GENOMES --attribute-value LAST_INDEX.TOPLEVEL_IDS.ensembl-version-number=37 ";
        attributes += "--attribute-value LAST_INDEX.INDEX.organism=HOMO_SAPIENS --attribute-value LAST_INDEX.INDEX.reference-build=1000GENOMES --attribute-value LAST_INDEX.INDEX.ensembl-version-number=37 ";
        attributes += "--attribute-value ENSEMBL_GENOMES.TOPLEVEL.organism=HOMO_SAPIENS --attribute-value ENSEMBL_GENOMES.TOPLEVEL.reference-build=1000GENOMES --attribute-value ENSEMBL_GENOMES.TOPLEVEL.ensembl-version-number=37 ";
        attributes += "--attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.organism=HOMO_SAPIENS --attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.reference-build=1000GENOMES --attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.ensembl-version-number=37 ";

        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(attributes + "--resource LAST_INDEX")));
        //assertTrue(new File("test-results/gateway-local-command-line/GOBYWEB_SGE_JOBS/junit/T/TJSOHOF/INDEX.properties").exists())
    }

    @Test
    public void installLocalMultipleResourceWithArtifactsAndAttributes() {
        String attributes = "--attribute-value LAST_INDEX.TOPLEVEL_IDS.organism=HOMO_SAPIENS --attribute-value LAST_INDEX.TOPLEVEL_IDS.reference-build=1000GENOMES --attribute-value LAST_INDEX.TOPLEVEL_IDS.ensembl-version-number=37 ";
        attributes += "--attribute-value LAST_INDEX.INDEX.organism=HOMO_SAPIENS --attribute-value LAST_INDEX.INDEX.reference-build=1000GENOMES --attribute-value LAST_INDEX.INDEX.ensembl-version-number=37 ";
        attributes += "--attribute-value ENSEMBL_GENOMES.TOPLEVEL.organism=HOMO_SAPIENS --attribute-value ENSEMBL_GENOMES.TOPLEVEL.reference-build=1000GENOMES --attribute-value ENSEMBL_GENOMES.TOPLEVEL.ensembl-version-number=37 ";
        attributes += "--attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.organism=HOMO_SAPIENS --attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.reference-build=1000GENOMES --attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.ensembl-version-number=37 ";

        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(attributes + "--resource LAST_INDEX --resource PLAST")));
        //assertTrue(new File("test-results/gateway-local-command-line/GOBYWEB_SGE_JOBS/junit/T/TJSOHOF/INDEX.properties").exists())
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
