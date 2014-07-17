package org.campagnelab.gobyweb.clustergateway.registration;

import junit.framework.Assert;
import org.campagnelab.gobyweb.clustergateway.util.JobMetadataParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Tester for {@link org.campagnelab.gobyweb.clustergateway.registration.StatefulFileSetRPCManager}
 *
 * @author manuele
 */
public class StatefulFileSetRPCManagerTest {

    StatefulFileSetRPCManager manager;


   // @Test
    public void testRegister() throws Exception {
       return;
    }

    //@Test
    public void testFetchMetadata() throws Exception {
      return;
    }

    //@Test
    public void testFetchStreamedEntry() throws Exception {
        List<ByteBuffer> data = new ArrayList<ByteBuffer>();
        List<String> errors = new ArrayList<String>();
        manager.fetchStreamedEntry("JOB_STATISTICS","ZGWYFYJ",data,errors);
        Assert.assertEquals(1,data.size());
        String propsAsString = new String(data.get(0).array());
        Properties props = new Properties();
        props.load(new StringReader(propsAsString));
        JobMetadataParser parser = new JobMetadataParser(props);
        Assert.assertTrue("Invalid job data fetched", parser.getAllRelatedInstancesTags().size() > 0);
    }

    //@Before
    public void setUp() throws Exception {
        manager = new StatefulFileSetRPCManager("spanky.med.cornell.edu",8849,"spanky.med.cornell.edu","gobyweb",
                "/zenodotus/campagnelab/store/data/gobyweb/dev/FILESET_AREA/","manuele.simi","JUnit");
        manager.connect();
    }

    //@After
    public void tearDown() throws Exception {
        manager.shutdown();

    }
}
