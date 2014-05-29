package org.campagnelab.gobyweb.clustergateway.util;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Properties;

/**
 * Created by mas2182 on 5/29/14.
 */
@RunWith(JUnit4.class)
public class JobMetadataParserTest {
    @Before
    public void setUp() {
        Properties properties = new Properties();
        properties.setProperty("JOB", "QNEEVXM");
        properties.setProperty("OWNER", "manuele.simi");
        properties.setProperty("COMPLETED", "2014-05-27 12:57:03-0400");
        properties.setProperty("TAGS", "OUTPUT_STATS:[FALODOD FALODOW FALODRE] READ_QUALITY_STATS:[VTWAHWU] WEIGHT_FILES:[VEJNIEU VEJNIEY] COMPACT_READ_FILES:[GJUZZFL]");
        parser = new JobMetadataParser(properties);

    }

    @Test
    public void testGetAllRelatedTags() {
        List<String> tags = parser.getAllRelatedInstancesTags();
        Assert.assertEquals("Invalid number of tags returned", 7, tags.size() );
    }

    private JobMetadataParser parser;
}
