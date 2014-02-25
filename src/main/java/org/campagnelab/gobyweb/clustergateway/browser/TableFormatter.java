package org.campagnelab.gobyweb.clustergateway.browser;

import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;

import java.util.*;

/**
 * Format the results of a browse operation in a table.
 *
 * @author manuele
 */
public final class TableFormatter implements OutputFormatter {

    Formatter formatter;
    String separator;

    public TableFormatter(String separator) {this.separator = separator;}

    public TableFormatter() {this("\t");}


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
        formatter.format("%n%-7s%s%-30s%s%-60s%s%-20s%s%-30s%s%-200s%n",
                "TAG",separator,
                "INSTANCE OF",separator,
                "BASENAME", separator,
                "SIZE (in bytes)", separator,
                "SHARED WITH", separator,
                "ATTRIBUTES");
        for (MetadataFileReader reader : metadataList) {
            formatter.format("%-7s", reader.getTag());
            formatter.format("%s", separator);
            formatter.format("%-30s", reader.getName());
            formatter.format("%s", separator);
            formatter.format("%-60s", reader.getBasename());
            formatter.format("%s", separator);
            long size = 0;
            for (String entry : reader.listEntryNames()) {
                size += reader.getEntrySize(entry);
            }
            formatter.format("%-20d", size);
            formatter.format("%s", separator);
            StringBuilder builder = new StringBuilder();
            for (String user: reader.getSharedWith())
                builder.append(user).append(",");
            formatter.format("%-30s", builder.toString().replaceAll(",$", ""));
            formatter.format("%s", separator);
            Iterator<Map.Entry<String, String>> it = reader.getAttributes().entrySet().iterator();
            builder = new StringBuilder();
            while (it.hasNext()) {
                Map.Entry<String,String> attribute = it.next();
                builder.append(String.format("%s=%s", attribute.getKey(), attribute.getValue()));
                if (it.hasNext())
                    builder.append(",");
            }
            formatter.format("%s", builder);
            formatter.format("%n");
        }
    }
}
