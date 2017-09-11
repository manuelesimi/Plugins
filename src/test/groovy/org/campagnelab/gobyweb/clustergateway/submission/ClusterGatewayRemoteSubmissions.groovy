package org.campagnelab.gobyweb.clustergateway.submission

import org.apache.commons.io.FileUtils
import org.junit.BeforeClass
import org.junit.Test

import static junit.framework.Assert.assertEquals

/**
 * Created by mas2182 on 12/5/16.
 */
class ClusterGatewayRemoteSubmissions {

    static final String pluginRoot = "/Users/mas2182/Lab/Projects/Git/gobyweb2-plugins";
    static final String envScript = "/Users/mas2182/Lab/Projects/Git/gobyweb2-plugins/artifacts-config/env.sh";

    static final String resultsDir = String.format("test-results/gateway-command-remote");
    static final String owner = "manuele.simi";

    static Properties prop = new Properties();

    @BeforeClass
    public static void configure() throws IOException {

        FileUtils.deleteDirectory(new File(resultsDir));
        FileUtils.forceMkdir(new File(resultsDir));
        try {
            prop.load(AlignerRemoteSubmissionTest.class.getResourceAsStream("/filtered.properties"));
        } catch (IOException e) {
            //assume we go ahead with the remote tests
            prop.setProperty("remoteTestSkip", "false");
        }
    }

    @Test
    public void runRemoteAligner() {
        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayRemoteSubmissions.runRemoteAligner() test");
            return;
        }
        assertEquals(0, ClusterGateway.process(buildClusterGatewayDarla()));
    }

    private static String[] buildClusterGatewayDarla() {
        ("--job-area gobyweb@spanky.pbtech:/home/gobyweb/darla-test/GOBYWEB_SGE_JOBS-dev " +
                "--fileset-area /home/gobyweb/darla-test/GOBYWEB_FILES-dev " +
                "--plugins-dir ${pluginRoot} " +
                "--owner ${owner} " +
                "--env-script ${envScript} "+
                "--queue fclab-debug.q " +
                "--GENOME_REFERENCE_ID WBcel215.69 " +
                "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                "--job BWA_GOBY_ARTIFACT " +
                "--artifact-server mas2182@mac162547.med.cornell.edu "+
                "INPUT_READS: AGSZAWW ").split(" ");

    }
}
