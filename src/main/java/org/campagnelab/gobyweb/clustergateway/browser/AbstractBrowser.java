package org.campagnelab.gobyweb.clustergateway.browser;

import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.io.FileSetArea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base browser. Provide common operations to all the browsers.
 *
 * @author manuele
 */
public abstract class AbstractBrowser implements FileSetBrowser {

    protected OutputFormatter formatter = null;

    /**
     * Looks for the single FileSet instance identified by the tag.
     *
     * @param area
     * @param tag
     */
    @Override
    public void browseByTag(FileSetArea area, String tag) throws IOException {
        if (this.formatter == null)
            throw new NullPointerException("OutputFormatter cannot be null. A formatter must be set before requesting any browse operation.");
        this.checksBeforeBrowse(area);
        FileSetAPI api =FileSetAPI.getReadOnlyAPI(area);
        List<String> errors = new ArrayList<String>();
        MetadataFileReader metadataFileReader = api.fetchMetadata(tag, errors);
        if (errors.size() > 0)
            this.formatter.formatErrors(errors);
        else {
            List<MetadataFileReader> metadataList = new ArrayList<MetadataFileReader>();
            metadataList.add(metadataFileReader);
            this.formatter.format(metadataList);
        }
        return;
    }

    /**
     * Sets the output formatter. Browse methods use the formatter to format the returned results
     *
     * @param formatter
     */
    @Override
    public void setOutputFormatter(OutputFormatter formatter) {
        this.formatter = formatter;
    }


    /**
     * Performs checks on the area before invoking any browse operation
     *
     * @param area
     * @throws IOException
     */
    protected abstract void checksBeforeBrowse(FileSetArea area) throws IOException;

}
