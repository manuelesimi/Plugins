package org.campagnelab.gobyweb.clustergateway.browser;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.io.FileSetArea;

import java.io.IOException;
import java.util.List;

/**
 * Browser for a remote fileset area.
 *
 * @author manuele
 */
public class RemoteBrowser extends AbstractBrowser  {

    RemoteBrowser() {}

    /**
     * Looks for FileSet instances matching the filters
     *
     * @param area
     * @param filters
     */
    @Override
    public void browseByFilters(FileSetArea area, List<FileSetAPI.AttributeFilter> filters) throws IOException {
        throw new UnsupportedOperationException("Remote browsing by filters is not allowed. Install the SDK on the remote node and perform a local browse.");
    }

    /**

    /**
     * Performes some checks before browsing the area.
     * @param area
     * @throws IOException
     */
    protected void checksBeforeBrowse(FileSetArea area) throws IOException {
        if (area.isLocal())
            throw new IOException("Cannot use a Remote Browser with a local area");
        if (this.formatter == null)
            throw new NullPointerException("OutputFormatter cannot be null. A formatter must be set before requesting any browse operation.");
    }
}
