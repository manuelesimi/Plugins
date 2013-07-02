package org.campagnelab.gobyweb.clustergateway.browser;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.io.FileSetArea;

import java.util.List;
import java.io.IOException;

/**
 * Base interface for fileset browsers.
 *
 * @author manuele
 */
interface FileSetBrowser {

    /**
     * Looks for the single FileSet instance identified by the tag
     * @param area
     * @param tag
     */
    public void browseByTag(FileSetArea area, String tag) throws IOException;

    /**
     * Looks for FileSet instances matching the filters
     * @param area
     * @param filters
     */
    public void browseByFilters(FileSetArea area, List<FileSetAPI.AttributeFilter> filters) throws IOException;

    /**
     * Sets the output formatter. Browse methods use the formatter to format the returned results
     * @param formatter
     */
    public void setOutputFormatter(OutputFormatter formatter);
}
