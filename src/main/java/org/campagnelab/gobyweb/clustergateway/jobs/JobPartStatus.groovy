package org.campagnelab.gobyweb.clustergateway.jobs

/**
 * Status for a part of a job submitted to the cluster.
 */
public enum JobPartStatus {

    // Try not to change existing numbers as they are stored in the database
    UNKNOWN("u"),
    START("b"),
    FAILED("f"),
    KILLED("k"),
    QUEUED("q"),
    SPLIT("s"),
    ALIGN("a"),
    SORT("o"),
    MERGE("m"),
    CONCAT("c"),
    COUNTS("n"),
    WIGGLES("w"),
    COMPRESS("z"),
    TRANSFER("t"),
    REGISTERED_FILESETS("r"),
    COMPLETED("d"),
    DIFF_EXP("x"),
    ALIGNMENT_SEQ_VARIATION_STATS("v"),
    ALIGNMENT_STATS("e")
    public String shortValue
    public String statusName
    public JobPartStatus(String shortValue) {
        this.shortValue = shortValue
        this.statusName = name().toLowerCase()
    }
    private static final Map<String, String> STATUS_NAME_TO_SHORT_VALUE = new HashMap<String, String>()
    private static final Map<String, String> SHORT_VALUE_TO_STATUS_NAME = new HashMap<String, String>()
    static {
        JobPartStatus.each { JobPartStatus partStatus ->
            STATUS_NAME_TO_SHORT_VALUE[partStatus.statusName] = partStatus.shortValue
            SHORT_VALUE_TO_STATUS_NAME[partStatus.shortValue] = partStatus.statusName
        }
    }

    public static String statusNameToShortValue(String value) {
        String result = STATUS_NAME_TO_SHORT_VALUE.get(value)
        if (result == null) {
            return JobPartStatus.UNKNOWN.shortValue
        } else {
            return result
        }
    }

    public static String shortValueToStatusName(String value) {
        String result = SHORT_VALUE_TO_STATUS_NAME.get(value)
        if (result == null) {
            return JobPartStatus.UNKNOWN.statusName
        } else {
            return result
        }
    }
}
