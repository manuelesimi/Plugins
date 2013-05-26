package org.campagnelab.gobyweb.clustergateway.submission;

import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.Plugins;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static junit.framework.Assert.fail;

/**
 * Test remote execution for aligners
 *
 * @author manuele
 */
//@RunWith(JUnit4.class)
public class AlignerRemoteSubmission {

    static Plugins plugins;
    static JobArea jobArea;
    static FileSetArea storageArea;
    static Actions actions;
    static final String filesetAreaReference = "/zenodotus/dat01/campagne_lab_store/gobyweb_dat/GOBYWEB_TRIAL/FILESETS_AREA";
    static final String jobAreaReference = "gobyweb@spanky.med.cornell.edu:/zenodotus/dat01/campagne_lab_scratch/gobyweb/GOBYWEB_TRIAL/SGE_JOBS";
    static final String envScript = "test-data/root-for-aligners/artifacts-config/env.sh";

    static final String owner = "campagne";
    //static String referenceSA =  new File(storageAreaDir).getAbsolutePath();


    //@BeforeClass
    public static void configure() {
        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-aligners");
        plugins.setWebServerHostname("localhost");
        plugins.reload();

        //create the reference to the job area
        try {
            jobArea = AreaFactory.createJobArea(jobAreaReference, owner);
        } catch (IOException ioe) {
            fail("failed to connect to the job area");
        }
    }

   // @Test
    public void submit() {
        try {
            Submitter submitter = new RemoteSubmitter(plugins.getRegistry(), "rascals.q");
            submitter.setSubmissionHostname(String.format("%s@%s",
                    System.getProperty("user.name"),
                    java.net.InetAddress.getLocalHost().getHostName()));
            submitter.setRemoteArtifactRepositoryPath("/scratchLocal/gobyweb/ARTIFACT_REPOSITORY-PLUGINS-SDK");
            submitter.setEnvironmentScript(new File(envScript).getAbsolutePath());
            actions = new Actions(submitter, filesetAreaReference, jobArea, plugins.getRegistry());
            actions.submitAligner(
                    "BWA_GOBY_ARTIFACT",
                    ClusterGateway.toInputParameters(new String[]{"INPUT_READS:", "SCGHGBF"}),
                    "WBcel215.69", //genome id
                    50000000, //chuck size
                    2, Collections.EMPTY_MAP); //number of parts
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed to submit a remote aligner for LAST_GOBY configuration");
        }
    }
}
