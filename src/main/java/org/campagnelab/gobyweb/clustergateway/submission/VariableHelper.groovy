package org.campagnelab.gobyweb.clustergateway.submission

import org.apache.log4j.Logger
import edu.cornell.med.icb.util.ICBStringUtils
import org.apache.commons.io.IOUtils

/**
 * Helps work with replacement maps.
 * @author Fabien Campagne
 * Date: 10/12/11
 * Time: 12:23 PM
 *
 */
public class VariableHelper {
    private static Logger LOG = Logger.getLogger(VariableHelper.class);

    /**
     * Write replacements as a bash script that defines environment variables for each key/value combination found in replacements.
     * @param replacements
     * @return
     */
    public File writeVariables(File output, final Map<String, Object> replacements) {
        FileWriter writer = null
        try {
            writer = new FileWriter(output)
            writeVariables(replacements, writer);
            writer.flush()
        } finally {
            IOUtils.closeQuietly(writer)
        }
        return output
    }

    public void writeVariables(final Map<String, Object> replacements, Writer writer) {
        replacements.keySet().each { constantName ->
            String key = constantName.replaceAll("%", "").toString()
            String value = replacements.get(constantName.toString()).toString()
            value=transform(value,replacements)
            if (value.contains("\n") || value.contains("\"")) {
                LOG.debug("Not writting variable ${key} to constants.sh because it contains new line or quote character. ");
            } else {
                writer.append("${key}=\"${value}\" \n");
            }
        }
    }

    public String getValue(String key, final Map<String, Object> replacements) {
        String value = replacements.get(key);
        transform(value, replacements);
    }

    public String transform(String value, final Map<String, Object> replacements) {
        int firstIndex = value.indexOf("%")
        if (firstIndex != -1) {
            int secondIndex = value.indexOf("%", firstIndex + 1)
            String containedKey = value.substring(firstIndex + 1, secondIndex)
            transform(substituteKeyValue(value, containedKey, replacements),
                    replacements)
        } else {
            return value
        }
    }

    private String substituteKeyValue(String value, String containedKey, Map<String, Object> replacements) {

        def decoratedKey = "%" + containedKey + "%"
        def keyValue = replacements.get(containedKey)
        if (keyValue==null) {
            LOG.warn("key was null, trying again with decorated key:"+decoratedKey);
            keyValue = replacements.get(decoratedKey).toString()
        }

        return value.replaceAll(decoratedKey, keyValue)
    }
}
