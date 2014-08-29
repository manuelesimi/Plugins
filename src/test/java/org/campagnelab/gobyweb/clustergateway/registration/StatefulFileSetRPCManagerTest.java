package org.campagnelab.gobyweb.clustergateway.registration;

import junit.framework.Assert;
import org.campagnelab.gobyweb.clustergateway.util.JobMetadataParser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tester for {@link org.campagnelab.gobyweb.clustergateway.registration.StatefulFileSetRPCManager}
 *
 * @author manuele
 */
public class StatefulFileSetRPCManagerTest {

    StatefulFileSetRPCManager manager;

    static Properties prop = new Properties();

    @BeforeClass
    public static void configure() {
        try {
            prop.load(StatefulFileSetRPCManagerTest.class.getResourceAsStream("/filtered.properties"));
        } catch (IOException e) {
            //assume we go ahead with the remote tests
            prop.setProperty("remoteTestSkip", "false");
        }
        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping AlignerRemoteSubmission test");
            return;
        }
    }

        // @Test
    public void testRegister() throws Exception {
       return;
    }

    @Test
    public void testFetchMetadata() throws Exception {
      return;
    }

    @Test
    public void download() throws Exception {
        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping AlignerRemoteSubmission.submit() test");
            return;
        }
        List<String> errors = new ArrayList<String>();
        List<String> entries = new ArrayList<String>();
        entries.add("INDEX");
        entries.add("HEADER");
        entries.add("ENTRIES");
        Map<String, List<String>> fetched = manager.download("faketag", entries, errors);
        Assert.assertEquals(0,fetched.size());

    }

    @Test
    public void testFetchStreamedEntry() throws Exception {
        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping AlignerRemoteSubmission.submit() test");
            return;
        }
        List<ByteBuffer> data = new ArrayList<ByteBuffer>();
        List<String> errors = new ArrayList<String>();
        manager.fetchStreamedEntry("JOB_STATISTICS","NITDQWR",data,errors);
        Assert.assertEquals(1,data.size());
        String propsAsString = new String(data.get(0).array());
        Properties props = new Properties();
        props.load(new StringReader(propsAsString));
        JobMetadataParser parser = new JobMetadataParser(props);
        Assert.assertTrue("Invalid job data fetched", parser.getAllRelatedInstancesTags().size() > 0);
    }

    @Before
    public void setUp() throws Exception {
        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping AlignerRemoteSubmission.submit() test");
            return;
        }
        manager = new StatefulFileSetRPCManager("spanky.med.cornell.edu",8849,"spanky.med.cornell.edu","gobyweb",
                "/zenodotus/campagnelab/store/data/gobyweb/trial/FILESET_AREA/","campagne","JUnit");
        manager.connect();
    }

    @After
    public void tearDown() throws Exception {
        if (prop.getProperty("remoteTestSkip").equalsIgnoreCase("true")) {
            System.out.println("Skipping AlignerRemoteSubmission.submit() test");
            return;
        }
        manager.shutdown();

    }
}
