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
        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXXX8 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads"
        )).size());

        Browser.process(buildBrowserArguments("XXXXXX8", "table"));

    }

    @Test
    public void testLocalBrowserByTagOnlyTags() {
        assertEquals(1, FileSetManager.process(FileSetCommandLineTest.buildFileRegistrationArgs(
                "--tag XXXXXX9 " +
                        "COMPACT_READS: test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/CASE2_FILE1.compact-reads"
        )).size());

        Browser.process(buildBrowserArguments("XXXXXX9","only-tags"));

    }

    private static String[] buildBrowserArguments(String tag, String format) {
        ("--fileset-area ${storageAreaDir} "+
                "--owner PluginsSDK "+
                "--tag ${tag} " +
                "--output-format ${format} "
        ).split(" ");
    }
}
