package org.campagnelab.gobyweb.clustergateway.submission

import org.apache.commons.io.FileUtils
import org.campagnelab.gobyweb.clustergateway.registration.FileSetManager

import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.*

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail

/**
 *
 * Test for FileSetManager command line
 * @author manuele
 *
 * Date: 4/1/13
 * Time: 4:04 PM
 */

@RunWith(JUnit4.class)
public class FileSetCommandLineTest {

    static String storageAreaDir = "test-results/filesets";

    @BeforeClass
    public static void configure() {
        FileUtils.deleteDirectory(new File(storageAreaDir));
        FileUtils.forceMkdir(new File(storageAreaDir));
    }

    @Test
    public void registerWithNameAndGuess() {
        assertEquals(5, FileSetManager.process(buildFileRegistrationArgs(
                "GOBY_ALIGNMENTS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.index "
                        + "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.entries "
                        + "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.header "
                        + "guess: "
                        + "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.compact-reads"
                )).size());

        assertEquals(5, FileSetManager.process(buildFileRegistrationArgs(
                "guess: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.index "
                        + "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.entries "
                        + "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.header "
                        + "guess: "
                        + "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.compact-reads"
        )).size());

        assertEquals(2, FileSetManager.process(buildFileRegistrationArgs(
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads" +
                " test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE2.compact-reads")).size());


    }

    @Test
    public void registerWithTags() {

        assertEquals(1, FileSetManager.process(buildFileRegistrationArgs(
                "--tag XXXXXX7 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads"
        )).size());

        try {
            //this has to fail because the input tag already exists (from the previous registration)
            assertEquals(0, FileSetManager.process(buildFileRegistrationArgs(
                    "--tag XXXXXX7 " +
                            "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads"
            )).size());
            fail("Registration of an existing tag did not fail as expected");
        } catch (Exception e) {/*expected*/}

        try {
            //this has to fail because we specify a tag but we try to upload 2 fileset instances
            assertEquals(0, FileSetManager.process(buildFileRegistrationArgs(
                "--tag XXXXXX8 " +
                "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads" +
                        " test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE2.compact-reads")).size());
            fail("Registration with tag one and two instances did not fail");
        } catch (Exception e) {/*expected*/}

    }

    @Test
    public void registerNoCopy() {
            //this has to fail because the input tag already exists (from the previous registration)
            assertEquals("Unexpected number of tags returned by registerNoCopy", 1, FileSetManager.process(buildFileRegistrationArgs(
                    "--no-copy " +
                    "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads"
            )).size());

    }

    private static String[] buildFileRegistrationArgs(String filenames) {
        ("--fileset-area ${storageAreaDir} "+
                "--plugins-dir test-data/root-for-rnaselect " +
                "--owner PluginsSDK "+
                "--action register " +
                filenames
        ).split(" ");

    }

}
