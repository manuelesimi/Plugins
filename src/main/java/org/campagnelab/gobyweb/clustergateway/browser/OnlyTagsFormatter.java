package org.campagnelab.gobyweb.clustergateway.browser;


import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;

import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Format the results of a browse operation by producing only list of tags for the matching filset instances.
 *
 * @author manuele
 */
public class OnlyTagsFormatter implements OutputFormatter {

    Formatter formatter;
    String separator;

    public OnlyTagsFormatter(String separator) {this.separator = separator;}

    public OnlyTagsFormatter() {this("\t");}
    /**
     * Formats the errors returned by a browse operation.
     *
     * @param errors
     */
    @Override
    public void formatErrors(List<String> errors) {
        formatter = new Formatter(System.err, Locale.US);
        for (String error : errors)
            formatter.format("%s", error);
    }

    /**
     * Formats the metadata list.
     *
     * @param metadataList
     */
    @Override
    public void format(List<MetadataFileReader> metadataList) {
        formatter = new Formatter(System.out);
        Iterator<MetadataFileReader> it = metadataList.iterator();
        while (it.hasNext()) {
            formatter.format("%s", it.next().getTag());
            if (it.hasNext())
                formatter.format("%s", separator);
        }
        formatter.format("%n");
    }
}
