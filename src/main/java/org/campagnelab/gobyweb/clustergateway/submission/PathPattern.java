package org.campagnelab.gobyweb.clustergateway.submission;

import com.esotericsoftware.wildcard.Paths;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manipulate and resolve path patterns.
 *
 * @author manuele
 */
public class PathPattern {

    private final String pattern;
    private final String searchDir;

    public PathPattern(String pattern) {
        this.pattern = extractFilePattern(pattern);
        this.searchDir = pattern.replace(this.pattern,"");
    }

    /**
     * Given a pattern in the form of SOME/PATH/PATTERN, return PATTERN
     * @param fullPattern
     * @return
     */
    private String extractFilePattern (String fullPattern) {
        Pattern p = Pattern.compile(".*?([^\\\\/]+)$");
        Matcher m = p.matcher(fullPattern);
        return (m.find()) ? m.group(1) : "";
    }

    /**
     * Scans the pattern and return the matching files.
      * @return
     */
    public List<File> scan() {
        Paths paths = new Paths().glob(searchDir,pattern);
        return paths.getFiles();
    }

}
