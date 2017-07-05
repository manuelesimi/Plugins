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

       assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs("--resource ENSEMBL_API:73")));

    }

    @Test
    public void runLocalTaskRNASelectNoServerHost() {
        List<String> tags = new ArrayList<String>();
        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads")));
        assertNotNull(tags);
        assertEquals(1, tags.size());

        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads")));

        assertEquals(4, tags.size());

        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(
                "--job RNASELECT_TASK "+
                "INPUT_READS: ${StringUtils.join(tags, ",")}","test-data/root-for-rnaselect")));

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
               "--job RNASELECT_TASK " +
               "INPUT_READS: ${StringUtils.join(tags, ",")}", "test-data/root-for-rnaselect")));

    }

    @Test
    public void runLocalTaskRNASelectWithTechnology() {
        List<String> tags = new ArrayList<String>();
        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads")));
        assertNotNull(tags);
        assertEquals(1, tags.size());

        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads")));

        assertEquals(4, tags.size());

        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(
                "--container_technology singularity --container_name artifacts/base --job RNASELECT_TASK " +
                "INPUT_READS: ${StringUtils.join(tags, ",")}", "test-data/root-for-rnaselect")));

    }


    @Test
    public void runLocalTaskRNASelectWithCloud() {
        List<String> tags = new ArrayList<String>();
        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads")));
        assertNotNull(tags);
        assertEquals(1, tags.size());

        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads")));

        assertEquals(4, tags.size());

        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(
                "--job RNASELECT_TASK --cloud-id test-cloud-id --cloud-secret tst-cloud-secret" +
                " INPUT_READS: ${StringUtils.join(tags, ",")}","test-data/root-for-rnaselect")));

    }

   //@Test
    public void runRemoteAligner() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAligner() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/campagnelab/scratch/data/gobyweb/dev/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA " +
                        "--plugins-dir ../gobyweb2-plugins " +
                        "--owner instructor " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job BWA_MEM_ARTIFACT " +
                        "--GENOME_REFERENCE_ID WBcel215.69 "+
                        "--CHUNK_SIZE 50000000 "+
                        "--option FOO=foo " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--depend-on FAKETAG5,FAKETAG6 " +
                        "--artifact-server ${artifactServer} "+
                       // "--broker-hostname ${brokerHostname} " +
                       // "--broker-port ${brokerPort} " +
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_READS: OTDDNIK"

                ).split(" ")
        ));

    }
    //@Test
    void runRemoteBWAAlignerSingularity() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAligner() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb3@darla.med.cornell.edu:/scratchLocal/gobyweb/gobyweb3/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /scratchLocal/gobyweb/gobyweb3/FILESET_AREA " +
                        "--plugins-dir ../gobyweb2-plugins " +
                        "--owner instructor " +
                        "--queue fclab-debug.q " +
                        "--env-script ${envScript} "+
                        "--job BWA_GOBY_ARTIFACT " +
                        "--container_technology singularity "+
                        "--container_name shub://CampagneLaboratory/GobyWeb-Singularity "+
                        "--GENOME_REFERENCE_ID 1000GENOMES.37 "+
                        "--CHUNK_SIZE 50000000 "+
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/gobyweb3/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_READS: ZTDMGNB"

                ).split(" ")
        ));

    }

    //@Test
    void runRemoteLastAlignerSingularity() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAligner() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb3@darla.med.cornell.edu:/scratchLocal/gobyweb/gobyweb3/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /scratchLocal/gobyweb/gobyweb3/FILESET_AREA " +
                        "--plugins-dir ../gobyweb2-plugins " +
                        "--owner instructor " +
                        "--queue fclab-debug.q " +
                        "--env-script ${envScript} "+
                        "--job LAST_NATIVE_PARALLEL_ARTIFACT " +
                        "--container_technology singularity "+
                 //       "--container_name /scratchLocal/gobyweb/gobyweb3/a2134bd109062d60855cf2f196e146f7f79ef0e6.img "+
                        "--container_name shub://CampagneLaboratory/GobyWeb-Singularity "+
                        "--container_memory 4 "+
                        "--GENOME_REFERENCE_ID 1000GENOMES.37 "+
                        "--CHUNK_SIZE 100000000 "+
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/gobyweb3/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_READS: ZTDMGNB --D 10 --E 10 --S 10"

                ).split(" ")
        ));

    }

    //@Test
    void runProcessReadsSingularity() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAligner() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb3@darla.med.cornell.edu:/scratchLocal/gobyweb/gobyweb3/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /scratchLocal/gobyweb/gobyweb3/FILESET_AREA " +
                        "--plugins-dir ../gobyweb2-plugins " +
                        "--owner instructor " +
                        "--queue fclab-debug.q " +
                        "--env-script ${envScript} "+
                        "--job PROCESS_READS_TASK " +
                        "--container_technology singularity "+
                        "--container_name shub://CampagneLaboratory/GobyWeb-Singularity "+
                        "--container_memory 4 "+
                        //   "--option QUEUE_WRITER_POSTFIX=--handler-service sampleService "+
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/gobyweb3/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "--SAMPLE_NAME HRPB-CTL-Male-Triplet-WGS " +
                        "--QUALITY_ENCODING Sanger " +
                        "--READS_PLATFORM Illumina " +
                        "--TAG TAWVUNS " +
                        "UPLOADS_FILES: ZTDMGNB " +
                        "UPLOAD_MERGE_PLAN: DZGPAXU").split(" ")))

    }

    //@Test
    void runRemoteAnalysis() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysis() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/campagnelab/scratch/data/gobyweb/dev/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner gobywebpaper " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job CONTAMINANT_EXTRACT " +
                        "--COMPARISON_PAIR Group_1/Group_2 "+
                        "--GROUP_DEFINITION Group_1=VMUKAAN,RHPHQGN " +
                        "--GROUP_DEFINITION Group_2=HGVOJLQ " +
                        "--option ENSEMBL_RELEASE=50 " +
                        //"--MERGE_GROUPS true " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_ALIGNMENTS: VMUKAAN HGVOJLQ RHPHQGN ALIGNMENT_SOURCE_READS: XJYTQZO HRFBTKJ SFQMOBF"
                ).split(" ")
        ));

    }
    //@Test
    public void runRemoteAnalysisWithOneGroup() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysisWithOneGroup() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(0, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/campagnelab/scratch/data/gobyweb/dev/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner gobywebpaper " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job CONTAMINANT_EXTRACT " +
                        "--COMPARISON_PAIR Group_1 "+
                        "--GROUP_DEFINITION Group_1= " +
                        "--option ENSEMBL_RELEASE=50 " +
                        //"--MERGE_GROUPS true " +
                        "--option BAR=bar " +
                        "--option BAZ=baz " +
                        "--option DEBUG=true " +
                        "--artifact-server ${artifactServer} "+
                        "--repository /scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK " +
                        "INPUT_ALIGNMENTS: VMUKAAN HGVOJLQ RHPHQGN ALIGNMENT_SOURCE_READS: XJYTQZO HRFBTKJ SFQMOBF"
                ).split(" ")
        ));

    }

    //@Test
    void runRemoteAnalysisWrongOptionValue() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysis() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(3, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/campagnelab/scratch/data/gobyweb/dev/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA " +
                        "--plugins-dir test-data/root-for-aligners " +
                        "--owner gobywebpaper " +
                        "--queue rascals.q " +
                        "--env-script ${envScript} "+
                        "--job CONTAMINANT_EXTRACT " +
                        "--SEARCH_REFERENCE foooooooo " +  //wrong value for search reference
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
    void runRemoteAnalysisWithWrongCardinality() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysisWithWrongCardinality() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(3, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/campagnelab/scratch/data/gobyweb/dev/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA " +
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

    //@Test
    void runRemoteAnalysisWithNotMatchingSlots() {

        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping ClusterGatewayCommandLineTest.runRemoteAnalysisWithNotMatchingSlots() test");
            return;
        }

        String artifactServer = String.format("%s@%s",
                System.getProperty("user.name"),
                java.net.InetAddress.getLocalHost().getHostName());
        assertEquals(3, ClusterGateway.process(
                ("--job-area gobyweb@spanky.med.cornell.edu:/zenodotus/campagnelab/scratch/data/gobyweb/dev/GOBYWEB_SGE_JOBS/ " +
                        "--fileset-area /zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA " +
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
    public void runTaskHelp() {
        assertEquals(1, ClusterGateway.process(
                ("--plugins-dir test-data/root-for-rnaselect "  +
                        "--job RNASELECT_TASK " +
                        "--help "
                ).split(" ")
        ));

    }

    @Test
    public void runAlignerHelp() {
        assertEquals(1, ClusterGateway.process(
                ("--plugins-dir test-data/root-for-aligners "  +
                        "--job BWA_GOBY_ARTIFACT " +
                        "--help "
                ).split(" ")
        ));

    }

    @Test(expected = Exception.class)
    public void runAlignerHelpWithAPI() {
       ClusterGateway.processAPI(
                ("--plugins-dir test-data/root-for-aligners "  +
                        "--job BWA_GOBY_ARTIFACT " +
                        "--help "
                ).split(" ")
        );

    }

    @Test
    public void runAnalysisHelp() {
        assertEquals(1, ClusterGateway.process(
                ("--plugins-dir test-data/root-for-aligners "  +
                 "--job CONTAMINANT_EXTRACT " +
                 "--help "
                ).split(" ")
        ));

    }

    @Test
    public void checkWrappersPaths() {
        List<String> tags = new ArrayList<String>();
        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads")));
        assertNotNull(tags);
        assertEquals(1, tags.size());

        tags.addAll(FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads")));

        assertEquals(4, tags.size());

        assertEquals(0, ClusterGateway.process(buildClusterGatewayArgs(
                "--job_wrappers_paths /Users/mas2182/Lab/Projects/Git/plugins-git/src/main/resources/c*.sh " +
                "--job_wrappers_paths /Users/mas2182/Lab/Projects/Git/plugins-git/src/main/resources/oge*.sh " +
                "--container_technology singularity --container_name artifacts/base --job RNASELECT_TASK",
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
                "--option FOO=foo " +
                "--option BAR=bar " +
                "--option BAZ=baz " +
                "--option DEBUG=true " +
                "--artifact-server localhost "+
                "--repository ${repoDirAbsolutePath} "+
                additionalCommands).split(" ");

    }
}
