package org.campagnelab.gobyweb.clustergateway.submission

import org.campagnelab.gobyweb.clustergateway.jobs.simulator.Option
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import static junit.framework.Assert.*


/**
 * Tester for {@link ClusterGatewaySimulator}.
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
class ClusterGatewaySimulatorTest {

    static final String gatewayPluginRoot = "test-data/root-for-aligners";

    @Test
    public void testAutoOptions() {
        SortedSet<Option> options = ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--job BWA_GOBY_ARTIFACT:1.2"),false)
        assertTrue(options.size() > 0) ;
    }

    @Test
    public void testResourceAutoOptionsWithAttributes() {
        String attributes = "--attribute-value ENSEMBL_GENOMES.TOPLEVEL.organism=caenorhabditis_elegans --attribute-value ENSEMBL_GENOMES.TOPLEVEL.reference-build=WBcel215 --attribute-value ENSEMBL_GENOMES.TOPLEVEL.ensembl-version-number=69 ";
        attributes += "--attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.organism=caenorhabditis_elegans --attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.reference-build=WBcel215 --attribute-value FAI_INDEXED_GENOMES.SAMTOOLS_FAI_INDEX.ensembl-version-number=69 ";
        attributes += "--attribute-value ENSEMBL_GTF.ANNOTATIONS.organism=caenorhabditis_elegans --attribute-value ENSEMBL_GTF.ANNOTATIONS.reference-build=WBcel215 --attribute-value ENSEMBL_GTF.ANNOTATIONS.ensembl-version-number=69 ";
        attributes += "--attribute-value STAR.INDEX.organism=caenorhabditis_elegans --attribute-value STAR.INDEX.reference-build=WBcel215 --attribute-value STAR.INDEX.ensembl-version-number=69 ";
        SortedSet<Option> options = ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs( attributes + "--resource STAR:2.3.0.6"),false);
        assertTrue(options.size() > 0) ;
        boolean found = false;
        for (Option option : options) {
            if (option.name.equals("RESOURCES_ARTIFACTS_STAR_INDEX_CAENORHABDITIS_ELEGANS_WBCEL215_69")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testAutoOptionsNoVersion() {
        SortedSet<Option> options = ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--job BWA_GOBY_ARTIFACT"),false);
        assertTrue(options.size() > 0) ;
    }

    @Test
    public void testResourceAutoOptionsNoVersion() {
        SortedSet<Option> options = ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--resource ANNOTATE_VCF"),false)
        assertTrue(options.size() > 0) ;
    }


    private static String[] buildClusterGatewaySimulatorArgs(String job) {

        ("--plugins-dir ${gatewayPluginRoot} " +
         "--action view-job-env " +
         "${job}").split(" ");
    }
}
