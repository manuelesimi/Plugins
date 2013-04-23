package org.campagnelab.gobyweb.clustergateway.registration;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.filesets.registration.InputEntry;
import org.campagnelab.gobyweb.filesets.registration.InputEntryListBuilder;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Command line interface for Fileset instance management.
 *
 * @author manuele
 */
public class FileSetManager {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(FileSetManager.class);

    private static CommandLineHelper jsapHelper = new CommandLineHelper(FileSetManager.class) {
        @Override
        protected boolean hasError(JSAPResult config, List<String> errors) {
            if (config.getString("action").equalsIgnoreCase("register")) {
                if (config.getStringArray("entries").length < 1) {
                    errors.add("Invalid list of fileset entries to register. At least one entry must be specified.");
                    return true;
                }
            } else if (config.getString("action").equalsIgnoreCase("unregister")) {
                if (!config.userSpecified("tag"))  {
                    errors.add("Missing tag parameter. Tag is needed to identify the fileset instance to unregister.");
                    return true;
                }
            }  else {
                errors.add("One action between register and unregister has to be specified");
                return true;
            }
            return false;
        }
     };

    public static void main(String[] args) {
        try {
            process(args);
            System.exit(0);
        } catch (Exception e) {
            logger.error(e);
            System.exit(1);
        }
    }

    public static List<String> process(String[] args) throws Exception {
        List<String> returned_values = new ArrayList<String>();
        JSAPResult config = jsapHelper.configure(args);
        if (config == null)
            System.exit(1);

        //create the reference to the storage area
        FileSetArea storageArea = null;
        try {
            storageArea = AreaFactory.createFileSetArea(
                    config.getString("fileset-area"),
                    config.userSpecified("owner")? config.getString("owner"): System.getProperty("user.name"));
        } catch (IOException ioe) {
            throw ioe;

        }

        //load plugin configurations
        Plugins plugins = null;
        try {
           // TODO: introduce a PluginHelper to do load and validate plugins. This part is common with ClusterGateway
            plugins = new Plugins();
            plugins.addServerConf(config.getFile("plugins-dir").getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            if (plugins.somePluginReportedErrors()) {
                System.err.println("Some plugins could not be loaded. See below for details. Aborting.");
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception();
        }
        try {
            List<String> errors = new ArrayList<String>();
            //convert plugins configuration to configurations that can be consumed by FileSetAPI
            ConfigurationList configurationList = PluginsToConfigurations.convertAsList(plugins.getRegistry().filterConfigs(FileSetConfig.class));
            FileSetAPI fileset = new FileSetAPI(storageArea,configurationList);
            if (config.getString("action").equalsIgnoreCase("register")) {
                returned_values = fileset.register(parseInputEntries(config.getStringArray("entries")),errors, config.getString("tag"));
                if (returned_values.size() > 0 ) {
                    logger.info(String.format("%d fileset instances have been successfully registered with the following tags: ", returned_values.size()));
                    logger.info(Arrays.toString(returned_values.toArray()));
                } else {
                    logger.error("Failed to register the fileset instances");
                    for (String message : errors) {
                        logger.error(message);
                    }
                    throw new Exception();
                }
            } else {
                fileset.unregister(config.getString("tag"));
                logger.info(String.format("Fileset instance %s successfully unregistered",config.getString("tag")));
            }
        } catch (IOException e) {
            throw new Exception();
        }
        return returned_values;
    }

    /**
     * Creates the input entries for the FileSet API
     * @param entries the list of entries as specified on the command line
     * @return the list of input entry
     * @throws Exception if any of the input entry does not have files associated
     */
    public static List<InputEntry> parseInputEntries(final String[] entries) throws Exception {
        List<InputEntry> inputEntries = new ArrayList<InputEntry>();
        String currentFilesetId = null;
        for (String entry : entries) {
            if (entry.endsWith(":")) {
                //move the current fileset id to this
                currentFilesetId = StringUtils.strip(entry, ":");
                continue;
            }
            InputEntry inputEntry;
            if (currentFilesetId == null || currentFilesetId.matches("guess")) {
                inputEntry = new InputEntry(entry);
            } else {
                inputEntry = new InputEntry(currentFilesetId, entry);
            }
            if (inputEntry.getFiles().size() > 0)
                inputEntries.add(inputEntry);
            else {
               String message = String.format("Invalid entry: %s does not have any file associated ", inputEntry.getHumanReadableName());
               logger.fatal(message);
               throw new Exception(message);
            }
        }
        return Collections.unmodifiableList(inputEntries);
    }

}
