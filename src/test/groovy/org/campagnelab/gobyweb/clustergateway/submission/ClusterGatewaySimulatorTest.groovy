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
       ClusterGatewaySimulator.process(buildClusterGatewaySimulatorArgs("BWA_GOBY_ARTIFACT:1.2"))
    }

    private static String[] buildClusterGatewaySimulatorArgs(String job) {

        ("--plugins-dir ${gatewayPluginRoot} " +
         "--action view-job-env " +
         "--job ${job}").split(" ");

    }
}
