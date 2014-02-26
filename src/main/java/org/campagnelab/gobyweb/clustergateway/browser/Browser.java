package org.campagnelab.gobyweb.clustergateway.browser;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.FileSetUtils;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.io.FileSetArea;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Browser interface.
 * It allows to browse the fileset area by tag of with filters on attributes.
 *
 * @author manuele
 */
public final class Browser {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(Browser.class);

    private static CommandLineHelper jsapHelper = new CommandLineHelper(Browser.class) {
        @Override
        protected boolean hasError(JSAPResult config, List<String> errors) {
            boolean result = false;
            if ((config.userSpecified("tag") ? 1 : 0) + (config.userSpecified("filter-attribute") ? 1 : 0) > 1) {
                errors.add("Only one parameter between tag and filter-attribute has to be specified");
                result =  true;
            }

            if ((config.userSpecified("tag") ? 1 : 0) + (config.userSpecified("filter-attribute") ? 1 : 0) < 1) {
                errors.add("One parameter between tag and filter-attribute has to be specified");
                result =  true;
            }

            if (config.userSpecified("fileset-area")) {
                String filesetAreadLocation = config.getString("fileset-area");
                if (filesetAreadLocation.contains(":")) {
                    String[] tokens = filesetAreadLocation.split(":");
                    if (tokens.length != 2) {
                        errors.add("Remote fileset-area must contain two tokens separated by :. Second token was found missing: " + filesetAreadLocation);
                        result = true;
                    } else {
                        filesetAreadLocation = tokens[1];
                        if (!new File(filesetAreadLocation).isAbsolute()) {
                            errors.add("--fileset-area parameter must be an absolute path " + filesetAreadLocation);
                            result = true;
                        } else if (! config.userSpecified("tag")) {
                            errors.add("Cannot use filters on a remote area. Install the SDK on the remote node and browse it locally.");
                            result = true;
                        }
                    }
                }
            }
            return result;
        }
    };

    /**
     * Entry point from the command line.
     * @param args the arguments
     */
    public static void main(String[] args) {
        try {
            process(args);
            System.exit(0);
        } catch (Exception e) {
            logger.error("Browser failed to process the request. " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Processes the caller requests.
     *
     * @param args the arguments passed on the command line
     * @return the list of tags in case of register action, an empty list for the other operations
     * @throws Exception if the request fails
     */
    public static void process(String[] args) throws Exception {
        JSAPResult config = jsapHelper.configure(args);
        if (config == null)
            System.exit(1);

        //create the reference to the storage area
        FileSetArea storageArea = null;
        try {
             storageArea = AreaFactory.createAdminFileSetArea(config.getString("fileset-area"),
                     config.userSpecified("owner")? config.getString("owner"): System.getProperty("user.name"));
        } catch (IOException ioe) {
            throw ioe;
        }
        FileSetBrowser browser = storageArea.isLocal()? new LocalBrowser() : new RemoteBrowser();
        browser.setOutputFormatter(getFormatter(config));
        if (config.userSpecified("tag")) {
           browser.browseByTag(storageArea,config.getString("tag"));
        } else if (config.userSpecified("filter-attribute")) {
            Map<String, List<String>> inputAttributes = parseKeyMultipleValueParameters(config.getStringArray("filter-attribute"));
            browser.browseByFilters(storageArea, FileSetUtils.toAttributeFilters(inputAttributes));
        }
    }

    /**
     * Creates a formatter to use starting from the input configuration.
     * @param config
     * @return the formatter
     * @throws Exception
     */
    private static OutputFormatter getFormatter(JSAPResult config) throws Exception {
        String format = config.getString("output-format");
        if (format.equalsIgnoreCase("table")) {
            if (config.userSpecified("separator"))
                return new TableFormatter(config.getString("separator"));
            else
                return new TableFormatter();
        }
        else if (format.equalsIgnoreCase("only-tags")) {
            if (config.userSpecified("separator"))
                return new OnlyTagsFormatter(config.getString("separator"));
            else
                return new OnlyTagsFormatter();
        } else
            throw new Exception(String.format("Unknown output format: %s. Allowed formats are [only-tags,table]", format));
    }

    /**
     * Parses the input parameters with multiple values and creates a map from them.
     * @param parameters  parameters in the form KEY=VALUE,VALUE2 KEY2=VALUE3
     * @return
     */
    public static Map<String, List<String>> parseKeyMultipleValueParameters(String[] parameters) throws Exception {
        if (parameters == null)
            return Collections.emptyMap();
        Map<String, List<String>> attributes = new HashMap<String, List<String>>();
        for (String inputAttribute: parameters) {
            String[] tokens = inputAttribute.split("=");
            if (tokens.length == 2) {
                attributes.put(tokens[0], Arrays.asList(tokens[1].split(",")));
            } else {
                logger.error("Invalid parameter format" + inputAttribute);
                throw new Exception();
            }
        }
        return attributes;
    }

}
