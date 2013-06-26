package org.campagnelab.gobyweb.clustergateway.datamodel

/**
 * Differential Expression data consumed by plugin scripts.
 *
 * @author manuele
 */
public class DiffExp {

    /**
     * Map of group number to the list of Alignments for that group. The Keys are in the format "m-n" where m is
     * the group name n is is the number of the alignment in the group, so if there are three
     * alignments in group 1, you will have three entries with keys "1-0", "1-1", "1-2".
     * This is a work-around because grails won't store a Map to a List.
     */
    Map<String, Alignment> grpToAligns;

    /**
     * Map of group number to group name.
     * Note that these entries are STRING (not the natural int) because Grails cannot store
     * a Map unless the key is a String. So if there are three groups, the group numbers keys
     * be "0", "1", "2".
     */
    Map<String, String> grpToName;

    /**
     * The options passed on the command line
     */
    Map<String, String> options;

    Integer numberOfGroups;

    String organismId;

    String referenceId;


    public DiffExp() {
        grpToAligns = new LinkedHashMap<String, Alignment>();
        grpToName = new LinkedHashMap<String, String>();
        options = new LinkedHashMap<String, String>();
    }

    public void addGroup(int groupNumber, String groupName) {
        grpToName.put(String.valueOf(groupNumber), groupName);
        numberOfGroups = grpToName.keySet().size();
    }

    public void addAlignmentToGroup(int groupIndex, int alignmentPosition, Alignment alignment) {
        grpToAligns.put(String.format("%d-%d",groupIndex, alignmentPosition),alignment);
    }

    /**
     * Lists all the alignments belonging to this analysis.
     * @return
     */
    public List<Alignment> allAlignments() {
        List<Alignment> result = new LinkedList<Alignment>()
        grpToAligns.each { String k, Alignment alignment ->
            result << alignment
        }
        return result
    }

    def alignmentsListForGroupNumber(int groupNum) {
        def result = []
        def expectedOrganism = organismId
        def expectedReference = referenceId
        grpToAligns.each {String k, Alignment v ->
            if (k.startsWith("${groupNum}-")) {
                def haveOrganism = v.getAttributes().get("ORGANISM")
                def haveReference = v.getAttributes().get("GENOME_REFERENCE_ID")
                if (expectedOrganism == haveOrganism && expectedReference == haveReference) {
                    result << v.getBasename()
                }
            }
        }
        return result
    }

    public Map<String, String> getOptions() {
        return options
    }

    public void setOptions(final Map<String, String> options) {
        this.options = options
    }
}
