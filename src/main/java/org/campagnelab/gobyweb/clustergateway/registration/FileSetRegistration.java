package org.campagnelab.gobyweb.clustergateway.registration;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import edu.cornell.med.icb.util.ICBStringUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line interface for Fileset instance registration.
 *
 *
 * @author manuele
 */
public class FileSetRegistration {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(FileSetRegistration.class);

    public static void main(String[] args) {
        JSAPResult config = loadConfig(args);

        //TODO: load pluginDir and StorageArea from the properties file if they are not specified as parameters

        //create the reference to the storage area
        FileSetArea storageArea = null;
        try {
            storageArea = AreaFactory.createFileSetArea(
                    config.getString("fileSetArea"), config.getString("owner"),
                    AreaFactory.MODE.valueOf(config.getString("mode").toUpperCase()));
        } catch (IOException ioe) {
           logger.error(ioe);
           System.exit(1);
        }

        //load plugin configurations
        Plugins plugins = null;
        try {
            plugins = new Plugins();
            plugins.addServerConf(config.getFile("pluginDir").getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
        } catch(Exception e) {
            e.printStackTrace();
            logger.error(e);
            System.exit(1);
        }
        try {
            Actions actions = new Actions(storageArea, plugins.getRegistry());
            if (config.getString("action").equalsIgnoreCase("register")) {
                String assignedTag = config.userSpecified("tag")? config.getString("tag") : ICBStringUtils.generateRandomString();
                actions.register(config.getString("id"), config.getStringArray("entries"), assignedTag);
                logger.info("Fileset instance successfully registered with tag " + assignedTag);
            }
            else {
                actions.unregister(config.getString("tag"));
                logger.info("Fileset instance successfully unregistered");
            }
        } catch (IOException e) {
            logger.error(e);
            System.exit(1);

        }
    }

    /**
     * Loads the parameters configuration and rules
     * @param args the command line arguments
     * @return the configuration
     */
    private static JSAPResult loadConfig(String[] args) {
        if (FileSetRegistration.class.getResource("FileSetRegistration.jsap") == null) {
            logger.fatal("unable to find the JSAP configuration file");
            System.err.println("unable to find the JSAP configuration file");
            System.exit(1);
        }
        JSAP jsap = null;
        try {
            jsap = new JSAP(FileSetRegistration.class.getResource("FileSetRegistration.jsap"));

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (JSAPException e) {
            e.printStackTrace();
            System.exit(1);
        }
        JSAPResult config = jsap.parse(args);
        List<String> errors = new ArrayList<String>();
        if (!config.success() || config.getBoolean("help") || hasActionError(config, errors)) {
            if (errors.size()>0) {
                for (String error : errors)
                    System.err.println("Error: " + error);
            }
            for (java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            System.err.println(jsap.getHelp());
            System.err.println();
            System.err.println("Usage: java " + FileSetRegistration.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.exit(0);
        }
        return config;
    }

    private static boolean hasActionError(JSAPResult config, List<String> errors) {
        if (config.getString("action").equalsIgnoreCase("register"))
            return hasRegisterError(config,errors);

        if (config.getString("action").equalsIgnoreCase("unregister"))
            return hasUnregisterError(config,errors);

        errors.add(String.format("Invalid action %s. Allowed actions: register | unregister", config.getString("action")));
        return true;
    }

    /**
     * Checks consistency of the unregistration parameters
     *
     * @param config
     * @param errors
     * @return
     */
    private static boolean hasUnregisterError(JSAPResult config, List<String> errors) {
        if (! config.getString("action").equalsIgnoreCase("register"))
            return false; //nothing to check here

        if (!config.userSpecified("tag"))
            errors.add("Missing --tag parameter. Tag is needed to identify the fileset instance to unregister.");

        return errors.size() > 0 ? true : false;
    }

    /**
     * Checks consistency of the registration parameters
     *
     * @param config
     * @param errors
     * @return
     */
    private static boolean hasRegisterError(JSAPResult config, List<String> errors) {
        if (! config.getString("action").equalsIgnoreCase("unregister"))
            return false; //nothing to check here

        if (!config.userSpecified("id"))
            errors.add("Missing --id parameter. Id must match the id reported in the configuration of the fileset.");

        if (config.getStringArray("entries").length < 1)
            errors.add("Missing entry list at the end of the input command. At least one entry must be registered for the fileset. An entry must be in the form ENTRY_NAME:ABSOLUTE PATH");

        return errors.size() > 0 ? true : false;
    }
}
