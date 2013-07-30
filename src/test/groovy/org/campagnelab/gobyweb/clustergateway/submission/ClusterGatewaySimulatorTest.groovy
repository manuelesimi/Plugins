package org.campagnelab.gobyweb.clustergateway.submission

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
       ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--job BWA_GOBY_ARTIFACT:1.2"),true)
    }

    @Test
    public void testResourceAutoOptions() {
        ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--resource ANNOTATE_VCF:1.1"),true)
    }

    @Test
    public void testAutoOptionsNoVersion() {
        ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--job BWA_GOBY_ARTIFACT"),true)
    }

    @Test
    public void testResourceAutoOptionsNoVersion() {
        ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("--resource ANNOTATE_VCF"),true)
    }


    private static String[] buildClusterGatewaySimulatorArgs(String job) {

        ("--plugins-dir ${gatewayPluginRoot} " +
         "--action view-job-env " +
         "${job}").split(" ");
    }
}
