package org.campagnelab.gobyweb.clustergateway.registration;

import com.google.common.base.Splitter;
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
import java.util.*;

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
            } else if ((config.getString("action").equalsIgnoreCase("unregister")
                      || (config.getString("action").equalsIgnoreCase("edit")))) {
                if (!config.userSpecified("tag"))  {
                    errors.add("Missing tag parameter. Tag is needed to identify the fileset instance to work with.");
                    return true;
                }
            }  else {
                errors.add("One action among register, edit or unregister has to be specified");
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
            logger.error("FileSetManager failed to process the request.",e);
            System.exit(1);
        }
    }

    /**
     * Processes the caller requests.
     * @param args the arguments passed on the command line
     * @return the list of tags in case of register action, an empty list for the other operations
     * @throws Exception
     */
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
            logger.error("Failed to load plugins definitions",e);
            throw new Exception(e);
        }

        List<String> errors = new ArrayList<String>();
        //convert plugins configuration to configurations that can be consumed by FileSetAPI
        ConfigurationList configurationList = PluginsToConfigurations.convertAsList(plugins.getRegistry().filterConfigs(FileSetConfig.class));
        FileSetAPI fileset = FileSetAPI.getReadWriteAPI(storageArea, configurationList);
        if (config.getString("action").equalsIgnoreCase("register")) {
            List<InputEntry> entries = parseInputEntries(config.getStringArray("entries"));
            if (config.userSpecified("no-copy"))
                returned_values = fileset.registerNoCopy(entries, parseInputAttributes(config.getStringArray("attribute")),
                        Arrays.asList(config.getStringArray("sharedWith")), errors, config.getString("tag"));
            else
                returned_values = fileset.register(entries, parseInputAttributes(config.getStringArray("attribute")),
                        Arrays.asList(config.getStringArray("sharedWith")), errors, config.getString("tag"));
            if (returned_values.size() > 0) {
                logger.info(String.format("%d fileset instances have been successfully registered with the following tags: ", returned_values.size()));
                logger.info(Arrays.toString(returned_values.toArray()));
            } else {
                logger.error("Failed to register the fileset instances");
                for (String message : errors) {
                    logger.error(message);
                }
                throw new Exception();
            }
        } else if (config.getString("action").equalsIgnoreCase("unregister")) {
            fileset.unregister(config.getString("tag"));
            logger.info(String.format("Fileset instance %s successfully unregistered", config.getString("tag")));
        } else if (config.getString("action").equalsIgnoreCase("edit")) {
            Map<String, String> attributes = parseInputAttributes(config.getStringArray("attribute"));
            if (attributes.keySet().size() > 0) {
                if (fileset.editAttributes(config.getString("tag"), attributes, errors)) {
                    logger.info(String.format("Fileset attributes have been successfully updated for instance %s", config.getString("tag")));
                } else {
                    logger.error("Failed to edit attributes.");
                    for (String message : errors) {
                        logger.error(message);
                    }
                    throw new Exception();
                }
            }
            if (config.getStringArray("sharedWith").length > 0) {
                if (fileset.editSharedUsers(config.getString("tag"), Arrays.asList(config.getStringArray("sharedWith")), errors)) {
                    logger.info(String.format("Fileset shared users have been successfully updated for instance %s", config.getString("tag")));
                } else {
                    logger.error("Failed to edit shared users.");
                    for (String message : errors) {
                        logger.error(message);
                    }
                    throw new Exception();
                }
            }

        }
        return returned_values;
    }

    /**
     * Parses the input attributes and creates a map from them.
     * @param inputAttributes  attributes in the form KEY=VALUE,KEY2=VALUE2
     * @return
     */
    public static Map<String, String>   parseInputAttributes(String[] inputAttributes) throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        if (inputAttributes == null)
            return attributes;

        for (String inputAttribute: inputAttributes) {
            String[] tokens = inputAttribute.split("=");
            if (tokens.length == 2) {
                attributes.put(tokens[0],tokens[1]);
            } else {
                logger.error("Invalid attribute format" + inputAttribute);
                throw new Exception();
            }
        }
        return attributes;
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
