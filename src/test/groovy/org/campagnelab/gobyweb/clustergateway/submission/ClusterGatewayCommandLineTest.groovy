package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4

import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertNotNull

/**
 * This is written in groovy because it is easier to build command lines on the fly in this language.
 * @author Fabien Campagne
 *         Date: 3/22/13
 *         Time: 1:58 PM
 */
@RunWith(JUnit4.class)
public class ClusterGatewayCommandLineTest {

    static final String gatewayPluginRoot = "test-data/root-for-gateway-command-line";
    static final String envScript = "test-data/env-scripts/env.sh";

    static final String resultsDir = String.format("test-results/gateway-command-line");
    static final String owner = "junit";
    static def repoDirAbsolutePath=new File("${resultsDir}/REPO").getAbsolutePath()


    @BeforeClass
    public static void configure() throws IOException {

        FileUtils.deleteDirectory(new File(resultsDir));
        FileUtils.forceMkdir(new File(resultsDir));

    }

    @Test
    public void installResourceWithArtifacts() {

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--resource MINIA:1.4961")));

    }

    @Test
    public void runLocalTaskRNASelect() {
       List<String> tags = new ArrayList<String>();
       tags.addAll(FileSetRegistration.process(buildFileRegistrationArgs(
               "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads")));
       assertNotNull(tags);
       assertEquals(1, tags.size());

       tags.addAll(FileSetRegistration.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads")));

       assertEquals(4, tags.size());

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(
               "--job RNASELECT_TASK",
               "test-data/root-for-rnaselect INPUT_READS: ${StringUtils.join(tags, ",")}")));

    }




    private static String[] buildFileRegistrationArgs(String filenames) {
        ("--fileset-area ${new File(resultsDir).getAbsolutePath()}/filesets "+
                "--plugins-dir test-data/root-for-rnaselect " +
                //"--owner ${owner} "+
                "--action register " +
                filenames
                ).split(" ");

    }
    private static String[] buildClusterGatewayArgs(String additionalCommands, String pluginRoot=gatewayPluginRoot) {
        ("--job-area ${new File(resultsDir).getAbsolutePath()}/GOBYWEB_SGE_JOBS " +
                "--fileset-area ${new File(resultsDir).getAbsolutePath()}/filesets " +
                "--plugins-dir ${pluginRoot} " +
                //"--owner ${owner} " +
                "--env-script ${envScript} "+
                "--artifact-server localhost "+
                "--repository ${repoDirAbsolutePath} "+
                additionalCommands).split(" ");

    }
}
