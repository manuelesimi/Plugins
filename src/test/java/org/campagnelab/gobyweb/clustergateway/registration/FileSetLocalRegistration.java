package org.campagnelab.gobyweb.clustergateway.registration;

import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test local registrations of fileset instances
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class FileSetLocalRegistration {

    private static Logger logger = Logger.getLogger(FileSetLocalRegistration.class);
    static Plugins plugins;
    static FileSetArea storageArea;
    static String storageAreaDir = String.format("test-results/filesets", System.currentTimeMillis());
    static Actions actions;
    private List<String> tags = new ArrayList<String>();


    @BeforeClass
    public static void configure() {
        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-rnaselect");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
        //create the reference to the storage area
        try {

            storageArea = AreaFactory.createFileSetArea(
                    storageAreaDir, "junit");
            actions = new Actions(storageArea, plugins.getRegistry());
        } catch (IOException ioe) {
            ioe.printStackTrace();
           fail("fail to create the local storage area");
        }
    }

    @Test
    public void registerFILESETPATH() {
       logger.debug("Testing registration CASE_1 (FILESET:PATH)");
       List<String> returnedTags = new ArrayList<String>();
       try {
            // CASE1: test with FILESET:path to file
           returnedTags.addAll(actions.register(
                   new String[]{"COMPACT_READS:", "CASE1_FILE1.compact-reads"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/")
            ));
        } catch (IOException e) {
            fail("fail to register fileset with FILESET:path");
        }
        assertEquals("Register operation returned an unexpected number of tags", 1, returnedTags.size());
        tags.addAll(returnedTags);

    }

    @Test
    public void registerFILESETPATTERN() {
        logger.debug("Testing registration CASE_2 (FILESET:PATTERN)");

        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with FILESET:pattern
            returnedTags.addAll(actions.register(
                    new String[]{"COMPACT_READS:", "*.compact-reads"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/")));
        } catch (IOException e) {
            fail("fail to register fileset with FILESET:pattern");
        }
        assertEquals("Register operation returned an unexpected number of tags", 3, returnedTags.size());
        tags.addAll(returnedTags);
    }

    @Test
    public void registerPATTERN() {
        logger.debug("Testing registration CASE_3 (PATTERN)");

        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with pattern
            returnedTags.addAll(actions.register(
                    new String[]{"*.compact-reads"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_3/")));
        } catch (IOException e) {
            fail("fail to register fileset with wildcard");
        }
        assertEquals("Register operation returned an unexpected number of tags", 2, returnedTags.size());
        tags.addAll(returnedTags);
    }

    @Test
    public void registerPATH() {
        logger.debug("Testing registration CASE_4 (PATH)");

        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with filename
            returnedTags.addAll(actions.register(
                    new String[]{"CASE4_FILE1.compact-reads"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_4/")));
        } catch (IOException e) {
            fail("fail to register fileset with filename");
        }
        assertEquals("Register operation returned an unexpected number of tags", 1, returnedTags.size());
        tags.addAll(returnedTags);
    }


    @Test
    public void registerMULTIPLEFILESETPATHS() {
        logger.debug("Testing registration CASE_5 (FILESET:PATHS)");
        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with pattern
            returnedTags.addAll(actions.register(
                    new String[]{"GOBY_ALIGNMENTS:","CASE5.index","CASE5.entries","CASE5.header"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_5/")));
        } catch (IOException e) {
            e.printStackTrace();
            fail("fail to register fileset with FILESET:PATHS");
        }
        assertEquals("Register operation returned an unexpected number of tags", 1, returnedTags.size());
        tags.addAll(returnedTags);
    }



    @Test
    public void registerMULTIPLEFILESETPATHSINCOMPLETE() {
        logger.debug("Testing registration CASE_6 (FILESET:PATHS, incomplete)");
        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with pattern
            returnedTags.addAll(actions.register(
                    new String[]{"GOBY_ALIGNMENTS:","CASE6_FILE1.index"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_6/")));
        } catch (IOException e) {
            e.printStackTrace();
            fail("fail to register fileset with incomplete FILESET:PATHS");
        }
        assertEquals("Register operation returned an unexpected number of tags", 0, returnedTags.size());
    }

    @Test
    public void registerMULTIPLEFILESETPATTERNS() {
        logger.debug("Testing registration CASE_7 (FILESET:PATTERNS)");
        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with pattern
            returnedTags.addAll(actions.register(
                    new String[]{"GOBY_ALIGNMENTS:", "*.index",  "*.entries", "*.header", "guess:", "*.compact-reads"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/")));
        } catch (IOException e) {
            e.printStackTrace();
            fail("fail to register fileset with FILESET:PATTERNS");
        }
        assertEquals("Register operation returned an unexpected number of tags", 5, returnedTags.size());
        tags.addAll(returnedTags);
    }

    @Test
    public void registerMULTIPLEFILESETPATTERNSINCOMPLETE() {
        logger.debug("Testing registration CASE_8 (FILESET:PATTERNS)");
        List<String> returnedTags = new ArrayList<String>();
        try {
            // test the case with pattern
            returnedTags.addAll(actions.register(
                    new String[]{"GOBY_ALIGNMENTS:", "*.index","*.entries","*.header", "guess:", "*.compact-reads"},
                    new File("test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_8/")));
        } catch (IOException e) {
            e.printStackTrace();
            fail("fail to register fileset with FILESET:PATTERNS");
        }
        assertEquals("Register operation returned an unexpected number of tags", 0, returnedTags.size());
        tags.addAll(returnedTags);
    }

    @Test
    public void check() {
        /*if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG1/AOUGEKP-Sample_MAN1.compact-reads").exists())
            fail("READS_FILE entry for TESTTAG1 not found");
        if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG1/.metadata/metadata.pb").exists())
            fail("metadata file for TESTTAG1 not found");
        if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG2/KHYMHVM-Sample_MAN2.compact-reads").exists())
            fail("READS_FILE entry for TESTTAG2 not found");
        if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG2/.metadata/metadata.pb").exists())
            fail("metadata file for TESTTAG2 not found");
        if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG3/OUTTRGH-Sample_MAN3.compact-reads").exists())
            fail("READS_FILE entry for TESTTAG3 not found");
        if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG3/.metadata/metadata.pb").exists())
            fail("metadata file for TESTTAG3 not found");  */
    }

    @Test
    public void unregister() {
        for (String tag : tags) {
            try {
                actions.unregister(tag);
            } catch (IOException e) {
                fail("failed to unregister fileset " + tag);
            }
        }

    }
    @Test(expected=IllegalArgumentException.class)
    public void wrongUnregister() {
        try {
            actions.unregister("FAKETAG");
        } catch (IOException e) {
            fail("wrong exception threw by unregistration");
        }
    }

    //@AfterClass
    public static void clean(){
       try {
           Files.deleteRecursively(new File(storageAreaDir).getParentFile());
        } catch (IOException e) {
           e.printStackTrace();
            fail("failed to delete the storage area");
        }
    }
}
