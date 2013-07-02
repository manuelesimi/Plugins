package org.campagnelab.gobyweb.clustergateway.browser;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.io.FileSetArea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Browser for a local fileset area.
 *
 * @author manuele
 */
public class LocalBrowser extends AbstractBrowser {

    public LocalBrowser() {}


    /**
     * Looks for FileSet instances matching the filters.
     *
     * @param area
     * @param filters
     */
    @Override
    public void browseByFilters(FileSetArea area, List<FileSetAPI.AttributeFilter> filters) throws IOException {
        if (this.formatter == null)
            throw new NullPointerException("OutputFormatter cannot be null. A formatter must be set before requesting any browse operation.");
        this.checksBeforeBrowse(area);
    }

    /**
     * Performes some checks before browsing the area.
     * @param area
     * @throws IOException
     */
    protected void checksBeforeBrowse(FileSetArea area) throws IOException {
        if (! area.isLocal())
            throw new IOException("Cannot use a Local Browser with a remote area");
    }
}
