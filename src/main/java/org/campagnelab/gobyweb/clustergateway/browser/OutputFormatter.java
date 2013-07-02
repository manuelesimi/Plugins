package org.campagnelab.gobyweb.clustergateway.browser;

import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;

import java.util.List;

/**
 * Interface for output formatters.
 * A formatter is responsible for presenting the information returned by a browse operation to the user.
 *
 * @author manuele
 */
interface OutputFormatter {

    /**
     * Formats the errors returned by a browse operation.
     * @param errors
     */
    void formatErrors(List<String> errors);

    /**
     * Formats the metadata list.
     * @param metadataList
     */
    void format(List<MetadataFileReader> metadataList);
}
