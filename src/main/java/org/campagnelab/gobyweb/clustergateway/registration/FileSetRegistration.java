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
import java.util.Arrays;
import java.util.List;

/**
 * Command line interface for Fileset instance registration.
 *
 * @author manuele
 */
public class FileSetRegistration {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(FileSetRegistration.class);

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
        JSAPResult config = loadConfig(args);
        if (config == null)
            System.exit(1);
        //TODO: load pluginDir and StorageArea from the properties file if they are not specified as parameters

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
            Actions actions = new Actions(storageArea, plugins.getRegistry());
            if (config.getString("action").equalsIgnoreCase("register")) {
                returned_values = actions.register(config.getStringArray("entries"),config.getFile("source-dir"));
                if (returned_values.size() > 0 ) {
                    logger.info(String.format("%d fileset instances have been successfully registered with the following tags: ", returned_values.size()));
                    logger.info(Arrays.toString(returned_values.toArray()));
                }
            } else {
                actions.unregister(config.getString("tag"));
                logger.info(String.format("Fileset instance %s successfully unregistered",config.getString("tag")));
            }
        } catch (IOException e) {
            throw new Exception();


        }
        return returned_values;
    }

    /**
     * Loads the parameters configuration and rules
     *
     * @param args the command line arguments
     * @return the configuration
     */
    private static JSAPResult loadConfig(String[] args) {
        if (FileSetRegistration.class.getResource("FileSetRegistration.jsap") == null) {
            logger.fatal("unable to find the JSAP configuration file");
            System.err.println("unable to find the JSAP configuration file");
            return null;
        }
        JSAP jsap = null;
        try {
            jsap = new JSAP(FileSetRegistration.class.getResource("FileSetRegistration.jsap"));

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSAPException e) {
            e.printStackTrace();
            return null;
        }
        JSAPResult config = jsap.parse(args);
        List<String> errors = new ArrayList<String>();
        if (!config.success() || config.getBoolean("help") || hasActionError(config, errors)) {
            if (errors.size() > 0) {
                for (String error : errors)
                    System.err.println("Error: " + error);
            }
            for (java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }
            System.err.println(jsap.getHelp());
            System.err.println();
            System.err.println("Usage: java " + FileSetRegistration.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            if (!config.success() ){ return null;}
        }
        return config;
    }

    private static boolean hasActionError(JSAPResult config, List<String> errors) {

        if (config.getString("action").equalsIgnoreCase("register"))
            return hasRegisterError(config, errors);

        if (config.getString("action").equalsIgnoreCase("unregister"))
            return hasUnregisterError(config, errors);

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
        if (!config.getString("action").equalsIgnoreCase("register"))
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
        if (!config.getString("action").equalsIgnoreCase("unregister"))
            return false; //nothing to check here

        if (!config.userSpecified("id"))
            errors.add("Missing --id parameter. Id must match the id reported in the configuration of the fileset.");

        if (config.getStringArray("entries").length < 1)
            errors.add("Missing the list of fileset entries to register. At least one entry must be registered.");

        return errors.size() > 0 ? true : false;
    }
}
