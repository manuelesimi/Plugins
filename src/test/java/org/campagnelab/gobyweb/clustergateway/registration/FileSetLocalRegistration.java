package org.campagnelab.gobyweb.clustergateway.registration;

import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.filesets.registration.InputEntry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
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
*  Test the compatibility of the fileset API with the FilesetManager
 * @author manuele
 */
@RunWith(JUnit4.class)
public class FileSetLocalRegistration {

    private static Logger logger = Logger.getLogger(FileSetLocalRegistration.class);
    static Plugins plugins;
    static FileSetArea storageArea;
    static String storageAreaDir = String.format("test-results/filesets", System.currentTimeMillis());
    static FileSetAPI fileset;
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
            fileset = new FileSetAPI(storageArea,
                    PluginsToConfigurations.convertAsList(plugins.getRegistry().filterConfigs(FileSetConfig.class)));
        } catch (IOException ioe) {
            ioe.printStackTrace();
           fail("fail to create the local storage area");
        }
    }

    @Test
    public void registerFILESETPATH() {
       String[] entries = new String[]{"COMPACT_READS:",
               "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads"};
       register(entries,"CASE_1","FILESET:PATH",1,0, false);
    }

    @Test
    public void registerWRONGENTRY() {
        String[] entries = new String[]{"COMPACT_READS:",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILExx.compact-reads"};
        register(entries,"CASE_1","FILESET:PATH, wrong entry",1,0, true);
    }

    @Test
    public void registerFILESETPATTERN() {
        String[] entries =  new String[]{"COMPACT_READS:",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_2/*.compact-reads"};
        register(entries,"CASE_2","FILESET:PATTERN",3,0, false);
    }

    @Test
    public void registerPATTERN() {
        String[] entries = new String[]{"test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_3/*.compact-reads"};
        register(entries,"CASE_3","PATTERN",2,0, false);
    }

    @Test
    public void registerPATH() {
        String[] entries = new String[]{"test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_4/CASE4_FILE1.compact-reads"};
        register(entries, "CASE_4", "PATH", 1, 0, false);
    }


    @Test
    public void registerMULTIPLEFILESETPATHS() {
        String[] entries =  new String[]{"GOBY_ALIGNMENTS:","test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_5/CASE5.index",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_5/CASE5.entries",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_5/CASE5.header"};
        register(entries,"CASE_5","FILESET:PATH",1,0, false);
    }

    @Test
    public void registerMULTIPLEFILESETPATHSINCOMPLETE() {
        String[] entries = new String[]{"GOBY_ALIGNMENTS:",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_6/CASE6_FILE1.index"};
        register(entries,"CASE_6","FILESET:PATHS, incomplete",0,1, false);
    }

    @Test
    public void registerMULTIPLEFILESETPATTERNS() {
        String[] entries = new String[]{"GOBY_ALIGNMENTS:",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.index",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.entries",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.header",
                "guess:", "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_7/*.compact-reads"};
        register(entries,"CASE_7", "FILESET:PATTERNS", 5, 0, false);
    }

    @Test
    public void registerMULTIPLEFILESETPATTERNSINCOMPLETE() {
        String[] entries = new String[]{"GOBY_ALIGNMENTS:",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_8/*.index",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_8/*.entries",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_8/*.header",
                "guess:", "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_8/*.compact-reads"};
        register(entries,"CASE_7", "FILESET:PATTERNS, one incomplete", 0, 1, false);
    }


    private void register(String[] entries, String caseID, String format,
                          int expectedTags, int expectedErrors, boolean parseShouldFail) {
        logger.debug(String.format("Testing registration %s (%s)",caseID,format));
        List<String> errors = new ArrayList<String>();
        List<String> returnedTags = new ArrayList<String>();
        List<InputEntry> inputEntries = null;
        try {
            inputEntries = FileSetManager.parseInputEntries(entries);
        } catch (Exception e) {
           if (parseShouldFail)
               return;
           else {
               e.printStackTrace();
               fail(String.format("Fail to parse input entry for fileset %s with %",caseID,format));
           }

        }
        try {
            // test the case
            returnedTags.addAll(fileset.register(inputEntries,errors, null));
        } catch (IOException e) {
            e.printStackTrace();
            fail(String.format("Fail to register fileset %s with %",caseID,format));
        }
        assertEquals(String.format("Register operation returned an unexpected number of tags using the format %s",format),
                expectedTags, returnedTags.size());
        assertEquals(String.format("Register operation returned an unexpected number of errors using the format %s",format),
                expectedErrors, errors.size());
        tags.addAll(returnedTags);
    }

    @Test
    public void unregister() {
        for (String tag : tags) {
            try {
                fileset.unregister(tag);
            } catch (IOException e) {
                fail("failed to unregister fileset " + tag);
            }
        }

    }
    @Test(expected=IllegalArgumentException.class)
    public void wrongUnregister() {
        try {
            fileset.unregister("FAKETAG");
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
