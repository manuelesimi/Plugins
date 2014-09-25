package org.campagnelab.gobyweb.clustergateway.registration;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author manuele
 */
@RunWith(JUnit4.class)
public class StatefulFileSetLocalManagerTest {

    StatefulFileSetLocalManager manager;
    Plugins plugins;
    String tag = "NEWTAGG";

    @Before
    public void setUp() throws Exception {
        FileUtils.deleteDirectory(new File("test-results/filesets"));
        manager = new StatefulFileSetLocalManager(String.format("test-results/filesets", System.currentTimeMillis()), "JUnit");
        plugins = new Plugins();
        plugins.replaceDefaultSchemaConfig(".");
        plugins.addServerConf("test-data/root-for-rnaselect");
        plugins.setWebServerHostname("localhost");
        plugins.reload();
        manager.setPluginDefinitions(plugins.getRegistry());
    }

    @Test
    public void testSequence() throws Exception {
        testRegister();
        testFetchStreamedEntry();
        testSharedWith();
    }


    private void testRegister() throws Exception {
       List<String> tags = manager.register("JOB_METADATA", new String[]{"test-data/cluster-gateway/files-for-registration-test/fileSets/JOB_METADATA/WENSREU.properties"},
               Collections.EMPTY_MAP,Collections.EMPTY_LIST, new ArrayList<String>(), tag);
        Assert.assertEquals(tags.size(),1);
    }


    private void testFetchStreamedEntry() throws Exception {
        List<ByteBuffer> data = new ArrayList<ByteBuffer>();
        List<String> errors = new ArrayList<String>();
        Assert.assertTrue("Unable to fetch streamed entry", manager.fetchStreamedEntry("JOB_STATISTICS", tag, data, errors));
        Assert.assertTrue("No data fetched", data.size() > 0);
        Properties props = new Properties();
        String propsAsString = new String(data.get(0).array());
        props.load(new StringReader(propsAsString));
        Assert.assertTrue("Invalid properties fetched", props.size() == 5);

    }

    private void testSharedWith() throws Exception {
        List<String> errors = new ArrayList<String>();
        MetadataFileReader metadata = manager.fetchMetadata(tag, errors);
        Assert.assertEquals(0, metadata.getSharedWith().size());
        List<String> users = new ArrayList<String>();
        users.add("manuele");
        users.add("fac2003");
        errors.clear();
        Assert.assertTrue(manager.shareWith(tag,users,errors));
        Assert.assertTrue("Some errors wrongly returned when trying to edit users", errors.size() == 0);
        errors.clear();
        metadata = manager.fetchMetadata(tag, errors);
        Assert.assertEquals(2, metadata.getSharedWith().size());
        Assert.assertEquals("manuele", metadata.getSharedWith().get(0));
        Assert.assertEquals("fac2003", metadata.getSharedWith().get(1));
    }
}
