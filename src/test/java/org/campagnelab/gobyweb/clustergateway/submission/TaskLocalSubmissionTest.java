package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.Plugins;

import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

/**
 * Test local task executions
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class TaskLocalSubmissionTest {

    static Plugins plugins;
    static JobArea jobArea;
    static FileSetArea storageArea;
    static Actions actions;
    static String brokerHostname = "localhost";
    static int brokerPort = 5672;
    static TaskConfig taskConfig;
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

        taskConfig = plugins.getRegistry().findByTypedId("RNASELECT_TASK", TaskConfig.class);
        assertNotNull(taskConfig);
    }

    @Test
    public void submit() {
        try {
            Submitter submitter = new LocalSubmitter(plugins.getRegistry());
            submitter.setSubmissionHostname("");
            submitter.setRemoteArtifactRepositoryPath("");
            submitter.assignTagToJob(ICBStringUtils.generateRandomString());
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry(),brokerHostname,brokerPort);
            actions.submitTask(taskConfig ,
                    SubmissionRequest.toInputParameters(new String[]{"INPUT_READS:", "TESTTAG1", "TESTTAG2", "TESTTAG3"}),
                    Collections.EMPTY_MAP);

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
            submitter.assignTagToJob(ICBStringUtils.generateRandomString());
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry(),brokerHostname,brokerPort);
            //12 values for input reads are not accepted
            actions.submitTask(
                    taskConfig,
                    SubmissionRequest.toInputParameters(new String[]{"INPUT_READS:",
                            "TESTTAG1", "TESTTAG2", "TESTTAG3", "TESTTAG1",
                            "TESTTAG2", "TESTTAG3", "TESTTAG1", "TESTTAG2",
                            "TESTTAG3", "TESTTAG1", "TESTTAG2", "TESTTAG3"}), Collections.EMPTY_MAP);
            fail("Exception must occur");
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
            submitter.assignTagToJob(ICBStringUtils.generateRandomString());
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry(),brokerHostname,brokerPort);
            //no values for input reads are not accepted
            actions.submitTask(
                    taskConfig,
                    SubmissionRequest.toInputParameters(new String[]{"INPUT_READS:",}), Collections.EMPTY_MAP);
            fail("Exception must occur");
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
            submitter.assignTagToJob(ICBStringUtils.generateRandomString());
            actions = new Actions(submitter, referenceSA, jobArea, plugins.getRegistry(),brokerHostname,brokerPort);
            //INPUT_READS slot is mandatory
            actions.submitTask(
                    taskConfig,
                    SubmissionRequest.toInputParameters(new String[]{}), Collections.EMPTY_MAP);
            fail("Exception must occur");
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
