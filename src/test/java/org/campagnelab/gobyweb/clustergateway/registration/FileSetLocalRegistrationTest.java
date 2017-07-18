package org.campagnelab.gobyweb.clustergateway.registration;

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.Broker;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.filesets.registration.core.BaseEntry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;


import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
*  Test the compatibility of the fileset API with the FilesetManager
 * @author manuele
 */
@RunWith(JUnit4.class)
public class FileSetLocalRegistrationTest {

    private static Logger logger = Logger.getLogger(FileSetLocalRegistrationTest.class);
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
            fileset =  FileSetAPI.getReadWriteAPI(storageArea,
                    PluginsToConfigurations.convertAsList(plugins.getRegistry().filterConfigs(FileSetConfig.class)));
        } catch (Exception ioe) {
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
        register(entries,"CASE_1","FILESET:PATH, wrong entry",0,1, true);
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


    private List<String> register(String[] entries, String caseID, String format,
                          int expectedTags, int expectedErrors, boolean parseShouldFail) {
        logger.debug(String.format("Testing registration %s (%s)",caseID,format));
        List<String> errors = new ArrayList<String>();
        List<String> returnedTags = new ArrayList<String>();
        List<BaseEntry> inputEntries = null;
        try {
            inputEntries = FileSetManager.parseInputEntries(entries);
        } catch (Exception e) {
           if (parseShouldFail)
               return Collections.emptyList();
           else {
               e.printStackTrace();
               fail(String.format("Fail to parse input entry for fileset %s with %",caseID,format));
           }

        }
        try {
            // test the case
            Broker broker = null;
            returnedTags.addAll(fileset.register(inputEntries, new HashMap<String, String>(), new ArrayList<String>(),errors, null, ""));
        } catch (IOException e) {
            e.printStackTrace();
            fail(String.format("Failed to register fileset %s with %",caseID,format));
        }
        assertEquals(String.format("Register operation returned an unexpected number of tags using the format %s",format),
                expectedTags, returnedTags.size());
        assertEquals(String.format("Register operation returned an unexpected number of errors using the format %s",format),
                expectedErrors, errors.size());
        tags.addAll(returnedTags);
        return returnedTags;
    }

    @Test
    public void unregister() {
        for (String tag : tags) {
            try {
                fileset.unregister(tag);
            } catch (IOException e) {
                fail("Failed to unregister fileset " + tag);
            }
        }

    }

    @Test
    public void editMetadata() {
        String[] entries = new String[]{"COMPACT_READS:",
                "test-data/cluster-gateway/files-for-registration-test/fileSets/CASE_1/CASE1_FILE1.compact-reads"};
        List<String> returnedTags = register(entries,"CASE_1","FILESET:PATH",1,0, false);
        List<String> errors = new ArrayList<String>();
        String[] attributes = new String[]{"KEY1=VALUE1", "KEY2=VALUE2", "KEY3=VALUE3", "KEY4=VALUE4"};
        try {
            if (!(fileset.editAttributes(returnedTags.get(0), FileSetManager.parseInputAttributes(attributes), errors))) {
                fail("Failed to edit fileset");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to edit fileset");
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
           Path directory = Paths.get(new File(storageAreaDir).getParentFile().getAbsolutePath());
           Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
               @Override
               public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                   Files.delete(file);
                   return FileVisitResult.CONTINUE;
               }

               @Override
               public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                   Files.delete(dir);
                   return FileVisitResult.CONTINUE;
               }
           });
       } catch (IOException e) {
           e.printStackTrace();
            fail("failed to delete the storage area");
        }
    }
}
