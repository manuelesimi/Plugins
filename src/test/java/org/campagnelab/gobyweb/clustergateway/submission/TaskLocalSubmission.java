package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.Plugins;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.fail;

/**
 * Test local task executions
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class TaskLocalSubmission {

    static Plugins plugins;
    static JobArea jobArea;
    static FileSetArea storageArea;
    static Actions actions;
    static final String sourceStorageAreaDir = "test-data/cluster-gateway/fileset-area-for-submission-test";
    static final String rootAreaDir = "test-results";
    static final String storageAreaDir = String.format("%s/filesets", rootAreaDir);
    static final String jobAreaDir = String.format("%s/jobs", rootAreaDir);
    static final String owner = "PluginsSDK";
    static String referenceSA =  new File(storageAreaDir).getAbsolutePath();



    @BeforeClass
    public static void configure() {
        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-rnaselect");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
        //prepare the storage area for testing
        try {
            //need to clone the storage area because results will be stored there too
            FileUtils.copyDirectory(new File(sourceStorageAreaDir).getAbsoluteFile(),
                    new File(storageAreaDir).getParentFile().getAbsoluteFile());
            storageArea = AreaFactory.createFileSetArea(
                    referenceSA, owner);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("failed to create the local storage area");
        }

        //create the reference to the job area
        try {
            jobArea = AreaFactory.createJobArea(new File(jobAreaDir).getAbsoluteFile().getAbsolutePath(), owner);
        } catch (IOException ioe) {
            fail("failed to create the local job area");
        }
    }

    @Test
    public void submit() {
        try {
            Submitter submitter = new LocalSubmitter(plugins.getRegistry());
            submitter.setSubmissionHostname("");
            submitter.setRemoteArtifactRepositoryPath("");
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry());
            actions.submitTask(
                    "RNASELECT_TASK",
                    ClusterGateway.toInputParameters(new String[]{"INPUT_READS:", "TESTTAG1", "TESTTAG2", "TESTTAG3"}));

        } catch (Exception e) {
            e.printStackTrace();
            fail("failed to submit a local task for RNASELECT_TASK configuration");
        }
    }

    @Test
    public void submitExpectedToFailTooManyValues() {
        try {
            Submitter submitter = new LocalSubmitter(plugins.getRegistry());
            submitter.setSubmissionHostname("");
            submitter.setRemoteArtifactRepositoryPath("");
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry());
            //12 values for input reads are not accepted
            actions.submitTask(
                    "RNASELECT_TASK",
                    ClusterGateway.toInputParameters(new String[]{"INPUT_READS:",
                            "TESTTAG1", "TESTTAG2", "TESTTAG3", "TESTTAG1",
                            "TESTTAG2", "TESTTAG3", "TESTTAG1", "TESTTAG2",
                            "TESTTAG3", "TESTTAG1", "TESTTAG2", "TESTTAG3"}));

        } catch (ExecutableJob.InvalidSlotValueException is) {
            //this is expected
        } catch (Exception e) {
            fail("unexpected exception received by job submission");
        }
    }

    @Test
    public void submitExpectedToFailTooFewValues() {
        try {
            Submitter submitter = new LocalSubmitter(plugins.getRegistry());
            submitter.setSubmissionHostname("");
            submitter.setRemoteArtifactRepositoryPath("");
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry());
            //no values for input reads are not accepted
            actions.submitTask(
                    "RNASELECT_TASK",
                    ClusterGateway.toInputParameters(new String[]{"INPUT_READS:",}));

        } catch (ExecutableJob.InvalidSlotValueException is) {
            //this is expected
        } catch (Exception e) {
            fail("unexpected exception received by job submission");
        }
    }

    @Test
    public void submitExpectedToFailMissingMandatorySlot() {
        try {
            Submitter submitter = new LocalSubmitter(plugins.getRegistry());
            submitter.setSubmissionHostname("");
            submitter.setRemoteArtifactRepositoryPath("");
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry());
            //INPUT_READS slot is mandatory
            actions.submitTask(
                    "RNASELECT_TASK",
                    ClusterGateway.toInputParameters(new String[]{}));

        } catch (ExecutableJob.InvalidJobDataException is) {
            //this is expected
        } catch (Exception e) {
            fail("unexpected exception received by job submission");
        }
    }


    //@AfterClass
    public static void clean() {
        try {
            Files.deleteRecursively(new File(rootAreaDir));
        } catch (IOException e) {
            e.printStackTrace();
            fail("failed to delete the test area");
        }
    }
}
