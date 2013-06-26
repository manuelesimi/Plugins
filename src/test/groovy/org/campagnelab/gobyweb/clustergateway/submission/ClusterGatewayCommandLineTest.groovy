package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.campagnelab.gobyweb.clustergateway.registration.FileSetManager
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
    static final String envScript = "test-data/root-for-aligners/artifacts-config/env.sh";

    static final String resultsDir = String.format("test-results/gateway-command-line");
    static final String owner = "junit";
    static def repoDirAbsolutePath=new File("${resultsDir}/REPO").getAbsolutePath()

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
    public void installResourceWithArtifacts() {

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--resource MINIA:1.4961")));

    }

    @Test
    public void runLocalTaskRNASelect() {
       List<String> tags = new ArrayList<String>();
       tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
               "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads")));
       assertNotNull(tags);
       assertEquals(1, tags.size());

       tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads")));

       assertEquals(4, tags.size());

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(
               "--job RNASELECT_TASK",
               "test-data/root-for-rnaselect INPUT_READS: ${StringUtils.join(tags, ",")}")));

    }

    @Test
    public void runRemoteAligner() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAligner() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/dat01/campagne_lab_scratch/gobyweb/GOBYWEB_TRIAL/SGE_JOBS " +
                        "--fileset-area /zenodotus/dat01/campagne_lab_store/gobyweb_dat/GOBYWEB_TRIAL/FILESETS_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner campagne " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job BWA_GOBY_ARTIFACT " +
                        "--GENOME_REFERENCE_ID WBcel215.69 "+
                        "--CHUNK_SIZE 50000000 "+
                        "--option FOO=foo " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_READS: KKHLEFC"

                ).split(" ")
        ));

    }

    @Test
    public void runRemoteAnalysis() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysis() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/dat01/campagne_lab_scratch/gobyweb/GOBYWEB_TRIAL/SGE_JOBS " +
                        "--fileset-area /zenodotus/dat01/campagne_lab_store/gobyweb_dat/GOBYWEB_TRIAL/FILESETS_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner gobywebpaper " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job CONTAMINANT_EXTRACT " +
                        "--COMPARISON_PAIR Group_1/Group_2 "+
                        "--GROUP_DEFINITION Group_1=ZDFTZZE,PVOVHCB " +
                        "--GROUP_DEFINITION Group_2=KAKIMJE " +
                        "--option ENSEMBL_RELEASE=50 " +
                        //"--MERGE_GROUPS true " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_ALIGNMENTS: KAKIMJE ZDFTZZE PVOVHCB ALIGNMENT_SOURCE_READS: XJYTQZO HRFBTKJ SFQMOBF"
                ).split(" ")
        ));

    }

    @Test
    public void runRemoteAnalysisWithWrongCardinality() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysisWithWrongCardinality() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(1, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/dat01/campagne_lab_scratch/gobyweb/GOBYWEB_TRIAL/SGE_JOBS " +
                        "--fileset-area /zenodotus/dat01/campagne_lab_store/gobyweb_dat/GOBYWEB_TRIAL/FILESETS_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner gobywebpaper " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job CONTAMINANT_EXTRACT " +
                        "--COMPARISON_PAIR Group_1/Group_2 "+
                        "--GROUP_DEFINITION Group_1=ZDFTZZE,PVOVHCB " +
                        "--GROUP_DEFINITION Group_2=KAKIMJE " +
                        "--option ENSEMBL_RELEASE=50 " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_ALIGNMENTS: KAKIMJE ZDFTZZE PVOVHCB ALIGNMENT_SOURCE_READS: XJYTQZO HRFBTKJ" //slots cardinality does not match
                ).split(" ")
        ));

    }

    @Test
    public void runRemoteAnalysisWithNotMatchingSlots() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysisWithNotMatchingSlots() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(1, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/dat01/campagne_lab_scratch/gobyweb/GOBYWEB_TRIAL/SGE_JOBS " +
                        "--fileset-area /zenodotus/dat01/campagne_lab_store/gobyweb_dat/GOBYWEB_TRIAL/FILESETS_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner gobywebpaper " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job CONTAMINANT_EXTRACT " +
                        "--COMPARISON_PAIR Group_1/Group_2 "+
                        "--GROUP_DEFINITION Group_1=ZDFTZZE,PVOVHCB " +
                        "--GROUP_DEFINITION Group_2=KAKIMJE " +
                        "--option ENSEMBL_RELEASE=50 " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_ALIGNMENTS: KAKIMJE ZDFTZZE PVOVHCB ALIGNMENT_SOURCE_READS: XJYTQZO HRFBTKJ KKHLEFC" //KKHLEFC does not match any source for alignments
                ).split(" ")
        ));

    }

    @Test
    public void runRemoteAnalysisHelp() {
        assertEquals(1, ClusterGateway.process(
                ("--plugins-dir test-data/root-for-aligners "  +
                 "--job CONTAMINANT_EXTRACT " +
                 "--help "
                ).split(" ")
        ));

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
                "--option FOO=foo " +
                "--option BAR=bar " +
                "--option BAZ=baz " +
                "--option DEBUG=true " +
                "--artifact-server localhost "+
                "--repository ${repoDirAbsolutePath} "+
                additionalCommands).split(" ");

    }
}
