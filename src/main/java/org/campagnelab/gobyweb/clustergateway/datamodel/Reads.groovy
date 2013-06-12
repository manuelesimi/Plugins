package org.campagnelab.gobyweb.clustergateway.datamodel

/**
 * Sample data consumed by plugin scripts.
 *
 * @author manuele
 */
public class Reads {

    private String path;

    private String tag;

    private String basename;

    /**
     * Attributes associated as metadata to the reads stored in the fileset area
     */
    private Map<String, String> attributes;


    public Reads(String tag) {
        this.tag = tag
        this.attributes = new LinkedHashMap<String, String>();

    }

    public String getPath() {
        this.path
    }

    public void setPath(String path) {
        this.path = path
    }

    public String getTag() {
        return tag
    }

    public String getBasename() {
        return basename
    }

    public void setBasename(String basename) {
        this.basename = basename
    }


    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes
    }

}
