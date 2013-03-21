package org.campagnelab.gobyweb.clustergateway.registration;

import com.google.common.io.Files;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.*;


import java.io.File;
import java.io.IOException;

/**
 * Test local registrations of fileset instances
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
public class FileSetLocalRegistration {

    static Plugins plugins;
    static FileSetArea storageArea;
    static String storageAreaDir = String.format("test-results-%d/filesets", System.currentTimeMillis());
    static Actions actions;

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
                    storageAreaDir, "ClusterGateway",
                    AreaFactory.MODE.LOCAL);
            actions = new Actions(storageArea, plugins.getRegistry());
        } catch (IOException ioe) {
            ioe.printStackTrace();
           fail("fail to create the local storage area");
        }
    }

    @Test
    public void register() {
        try {
            actions.register("COMPACT_READS",
                    new String[]{"READS_FILE:test-data/cluster-gateway/files-for-registration-test/fileSets/READS_1/AOUGEKP-Sample_MAN1.compact-reads"},
                    "TESTTAG1");
        } catch (IOException e) {
            fail("fail to register fileset TESTTAG1");
        }
        try {
            actions.register("COMPACT_READS",
                    new String[]{"READS_FILE:test-data/cluster-gateway/files-for-registration-test/fileSets/READS_2/KHYMHVM-Sample_MAN2.compact-reads"},
                    "TESTTAG2");
        } catch (IOException e) {
            fail("fail to register fileset TESTTAG2");
        }

        try {
            actions.register("COMPACT_READS",
                    new String[]{"READS_FILE:test-data/cluster-gateway/files-for-registration-test/fileSets/READS_3/OUTTRGH-Sample_MAN3.compact-reads"},
                    "TESTTAG3");
        } catch (IOException e) {
            fail("fail to register fileset TESTTAG3");
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void duplicateRegister() {
        try {
            actions.register("COMPACT_READS",
                    new String[]{"READS_FILE:test-data/cluster-gateway/files-for-registration-test/READS_1/AOUGEKP-Sample_MAN1.compact-reads"},
                    "TESTTAG1");
        } catch (IOException e) {
            fail("wrong exception threw by registration");
        }
    }

    @Test
    public void check() {
        if (! new File(storageAreaDir+"/ClusterGateway/TESTTAG1/AOUGEKP-Sample_MAN1.compact-reads").exists())
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
            fail("metadata file for TESTTAG3 not found");
    }

    @Test
    public void unregister() {
        try {
            actions.unregister("TESTTAG1");
        } catch (IOException e) {
            fail("failed to unregister fileset TESTTAG1");
        }
        try {
            actions.unregister("TESTTAG2");
        } catch (IOException e) {
            fail("failed to unregister fileset TESTTAG2");
        }
        try {
            actions.unregister("TESTTAG3");
        } catch (IOException e) {
            fail("failed to unregister fileset TESTTAG3");
        }

    }
    @Test(expected=IllegalArgumentException.class)
    public void wrongUnregister() {
        try {
            actions.unregister("TESTTAG4");
        } catch (IOException e) {
            fail("wrong exception threw by unregistration");
        }
    }

    @AfterClass
    public static void clean(){
       try {
           Files.deleteRecursively(new File(storageAreaDir).getParentFile());
        } catch (IOException e) {
           e.printStackTrace();
            fail("failed to delete the storage area");
        }
    }
}
