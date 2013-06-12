package org.campagnelab.gobyweb.clustergateway.datamodel

/**
 * Alignment data consumed by plugin scripts.
 *
 * @author manuele
 */
class Alignment {

    private Reads reads;

    private String tag;

    private String basename;

    /**
     * Attributes associated as metadata to the alignment stored in the fileset area
     */
    private Map<String, String> attributes;

    //this inner class if for backward compatibility with previous GobyWeb pre-deployment scripts
    AlignJob alignJob;


    public Alignment(String tag) {
      this.tag = tag;
      this.alignJob = new AlignJob();
      this.attributes = new LinkedHashMap<String, String>();
    }

    public void setReads(Reads reads) {
        this.reads = reads;
    }

    public Reads getReads() {
        this.reads;
    }

    public String getBasename() {
        return basename
    }

    public void setBasename(String basename) {
        this.basename = basename
    }

    public void setAlignJobTag(String tag) {
        alignJob.tag = tag;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }


    class AlignJob {

        public String tag;
    }
}
