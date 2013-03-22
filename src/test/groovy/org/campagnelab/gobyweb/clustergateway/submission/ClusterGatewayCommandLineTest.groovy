package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils
import org.campagnelab.gobyweb.clustergateway.registration.FileSetRegistration
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4

import static junit.framework.Assert.assertEquals

/**
 * This is written in groovy because it is easier to build command lines on the fly in this language.
 * @author Fabien Campagne
 *         Date: 3/22/13
 *         Time: 1:58 PM
 */
@RunWith(JUnit4.class)
public class ClusterGatewayCommandLineTest {

    static final String sourceStorageAreaDir = "test-data/cluster-gateway/command-line-tests";
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

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("local","--resource MINIA:1.4961")));

    }

    @Test
    public void runLocalTaskRNASelect() {

       assertEquals(0, FileSetRegistration.process(buildFileRegistrationArgs("local",
               "COMPACT_READS",
               "READS_FILE:test-data/cluster-gateway/files-for-registration-test/fileSets/READS_1/AOUGEKP-Sample_MAN1.compact-reads",
               "TESTTAG1")));
        assertEquals(0, FileSetRegistration.process(buildFileRegistrationArgs("local",
                "COMPACT_READS",
               "READS_FILE:test-data/cluster-gateway/files-for-registration-test/fileSets/READS_2/KHYMHVM-Sample_MAN2.compact-reads",
               "TESTTAG2")));
        assertEquals(0, FileSetRegistration.process(buildFileRegistrationArgs("local",
                "COMPACT_READS",
               "READS_FILE:test-data/cluster-gateway/files-for-registration-test/fileSets/READS_3/OUTTRGH-Sample_MAN3.compact-reads",
               "TESTTAG3")));

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("local",
               "--input-filesets:TESTTAG1,TESTTAG2,TESTTAG3 --task RNASELECT_TASK",
               "test-data/root-for-rnaselect")));

    }




    private static String[] buildFileRegistrationArgs(String remoteLocal, String filesetId, String filenames, String tag) {
        ("--fileset-area ${resultsDir}/filesets "+
                "--plugins-dir test-data/root-for-rnaselect " +
                "--fileset-id ${filesetId} " +
                "--owner ${owner} "+
                "--mode ${remoteLocal} " +
                "--tag ${tag} " +
                "--action register " +
                filenames
                ).split(" ");

    }
    private static String[] buildClusterGatewayArgs(String remoteLocal, String additionalCommands, String pluginRoot=gatewayPluginRoot) {
        ("--job-area ${resultsDir}/GOBYWEB_SGE_JOBS " +
                "--fileset-area ${resultsDir}/filesets " +
                "--plugins-dir ${pluginRoot} " +
                "--owner ${owner} " +
                "--env-script ${envScript} "+
                "--mode ${remoteLocal} " +
                "--artifact-server localhost "+
                "--repository ${repoDirAbsolutePath} "+
                additionalCommands).split(" ");

    }
}
