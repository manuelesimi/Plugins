package org.campagnelab.gobyweb.browser

import org.apache.commons.io.FileUtils
import org.campagnelab.gobyweb.clustergateway.browser.Browser
import org.campagnelab.gobyweb.clustergateway.registration.FileSetManager
import org.campagnelab.gobyweb.clustergateway.submission.FileSetCommandLineTest
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test

import static junit.framework.Assert.assertEquals


/**
 * Tester for browse operation
 * @author manuele
 */
@RunWith(JUnit4.class)
class BrowserCommandLineTest {

    static String storageAreaDir = "test-results/filesets";

    @BeforeClass
    public static void configure() {
        FileUtils.deleteDirectory(new File(storageAreaDir));
        FileUtils.forceMkdir(new File(storageAreaDir));
    }

    @Test
    public void testLocalBrowserByTagWithTable() {
        String[] attributes = new String[1];
        attributes[0] = "KEY1=VALUE1";

        String[] users = new String[3];
        users[0] = "me";
        users[1] = "myself";
        users[2] = "I";

        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXXX8 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads",
                attributes, users
        )).size());

        Browser.process(buildBrowserArguments("XXXXXX8", "table"));

    }

    @Test
    public void testLocalBrowserByTagOnlyTags() {
        String[] attributes = new String[1];
        attributes[0] = "KEY1=VALUE1";

        String[] users = new String[3];
        users[0] = "me";
        users[1] = "myself";
        users[2] = "I";

        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXXX9 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads",
                attributes, users
        )).size());

        Browser.process(buildBrowserArguments("XXXXXX9","only-tags"));

    }

    @Test
    public void testLocalBrowserByFiltersWithTable() {
        String[] attributes = new String[4];
        attributes[0] = "KEY1=VALUE1";
        attributes[1] = "KEY2=VALUE2";
        attributes[2] = "KEY3=VALUE3";
        attributes[3] = "KEY4=VALUE4";

        String[] users = new String[3];
        users[0] = "me";
        users[1] = "myself";
        users[2] = "I";

        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXX10 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads",
                attributes, users
        )).size());

        String[] attributes2 = new String[2];
        attributes2[0] = "KEY1=VALUE1";
        attributes2[1] = "KEY2=VALUE2";

        String[] users2 = new String[1];
        users2[0] = "me";


        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXX11 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads",
                attributes2, users2
        )).size());

        String[] attributes3 = new String[3];
        attributes3[0] = "KEY1=VALUE1";
        attributes3[1] = "KEY2=VALUE2";
        attributes3[2] = "KEY3=VALUE3";
        String[] users3 = new String[1];
        users3[0] = "me";

        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXX12 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads",
                attributes3, users3
        )).size());

        Browser.process(buildBrowserFiltersArguments(["KEY1=VALUE1", "KEY3=VALUE3"] as String[],"table"));

    }

    private static String[] buildBrowserFiltersArguments(String[] filters, String format) {
        StringBuilder builder = new StringBuilder();
        for(String filter : filters)
            builder.append("--filter-attribute ${filter} ")

        ("--fileset-area ${storageAreaDir} "+
                "--owner PluginsSDK "+
                "--output-format ${format} " +
                builder.toString()
        ).split(" ");
    }

    private static String[] buildBrowserArguments(String tag, String format) {
        ("--fileset-area ${storageAreaDir} "+
                "--owner PluginsSDK "+
                "--tag ${tag} " +
                "--output-format ${format} "
        ).split(" ");
    }
}
