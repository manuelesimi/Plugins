package org.campagnelab.gobyweb.clustergateway.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to parse JOB_STATISTICS properties published in a JOB_METADATA fileset instance.
 *
 * @author manuele
 */
public class JobMetadataParser {

    private final Properties properties;

    private static final Pattern tagsPattern = Pattern.compile("(.*?):\\[(.*?)]");


    public JobMetadataParser(Properties properties) {
        this.properties = properties;
    }

    public JobMetadataParser(File file) throws IOException {
        this.properties = new Properties();
        InputStream input = new FileInputStream(file);
        this.properties.load(input);
    }

    /**
     * Gets the tags of all the fileset instances published by the job.
     * @return
     */
    public List<String> getAllRelatedInstancesTags(){
        List<String> tags = new ArrayList<String>();
        String tagsProp = this.properties.getProperty("TAGS");
        Matcher matcher = tagsPattern.matcher(tagsProp);
        while (matcher.find()) {
            for (String tag : matcher.group(2).split(" "))
                tags.add(tag);
        }
        return tags;
    }
}
