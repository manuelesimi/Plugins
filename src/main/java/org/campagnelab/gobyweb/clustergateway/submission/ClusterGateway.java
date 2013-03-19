package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line interface to the cluster gateway
 *
 *
 * @author manuele
 */
public class ClusterGateway {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(ClusterGateway.class);

    public static void main(String[] args) {
        JSAPResult config = loadConfig(args);

        //TODO: load pluginDir and StorageArea from the properties file if they are not specified as parameters

        //create the reference to the storage area
        FileSetArea storageArea = null;
        try {
            storageArea = AreaFactory.createFileSetArea(
                    config.getString("fileSetArea"), config.getString("owner"));
        } catch (IOException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }


        //create the reference to the job area
        JobArea jobArea = null;
        try {
            jobArea = new JobArea(config.getString("jobArea"), config.getString("owner"));
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
            Actions actions = new Actions(storageArea, jobArea, plugins.getRegistry());
            if (config.getString("mode").equalsIgnoreCase("remote"))   {
                if (!jobArea.isLocal())
                    actions.submitRemoteTask(config.getString("queue"),
                            config.getString("task"),
                            config.getStringArray("inputFilesets")
                    );
                else
                    logger.error("Cannot use the remote submitter with a local job area");
            } else if (config.getString("mode").equalsIgnoreCase("local")) {
                if (jobArea.isLocal())
                    actions.submitLocalTask(
                            config.getString("task"),
                            config.getStringArray("inputFilesets")
                    );
                else
                    logger.error("Cannot use the local submitter with a remote job area");
            }
        } catch (Exception e) {
            logger.error("Failed to manage the requested action", e);
            System.exit(1);

        }
    }


    /**
     * Loads the parameters configuration and rules
     * @param args the command line arguments
     * @return the configuration
     */
    private static JSAPResult loadConfig(String[] args) {
        if (ClusterGateway.class.getResource("ClusterGateway.jsap") == null) {
            logger.fatal("unable to find the JSAP configuration file");
            System.err.println("unable to find the JSAP configuration file");
            System.exit(1);
        }
        JSAP jsap = null;
        try {
            jsap = new JSAP(ClusterGateway.class.getResource("ClusterGateway.jsap"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (JSAPException e) {
            e.printStackTrace();
            System.exit(1);

        }
        List<String> errors = new ArrayList<String>();
        JSAPResult config = jsap.parse(args);
        if (config.userSpecified("help") || hasError(config, errors)) {
            if (errors.size()>0) {
                for (String error : errors)
                    System.err.println("Error: " + error);
            }
            for (java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }
            System.err.println(jsap.getHelp());
            System.err.println();
            System.err.println("Usage: java " + ClusterGateway.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();
            System.exit(0);
          }
        return config;
    }

    private static boolean hasError(JSAPResult config, List<String> errors) {

        if (config.getString("mode").equalsIgnoreCase("local"))
              return false;

        if (config.getString("mode").equalsIgnoreCase("remote"))   {
            if (!config.userSpecified("queue")) {
                errors.add("No queue has been indicated and none was found in the default configuration properties");
                return true;
            }
            return false;
        }
        errors.add(String.format("Invalid mode %s. Allowed modes: local | remote", config.getString("mode")));
        return true;
    }

}
