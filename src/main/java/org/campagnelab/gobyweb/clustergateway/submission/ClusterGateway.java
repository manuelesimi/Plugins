package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.io.JobArea;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line interface to the cluster gateway
 *
 * @author manuele
 */
public class ClusterGateway {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(ClusterGateway.class);


    public static void main(String[] args) {
        System.exit(process(args));

    }

    public static int process(String[] args) {
        JSAPResult config = loadConfig(args);
        if (config == null) return 1;
        //TODO: load pluginDir and StorageArea from the properties file if they are not specified as parameters

        //create the reference to the storage area
        FileSetArea storageArea = null;
        try {
            storageArea = AreaFactory.createFileSetArea(
                    config.getString("fileSetArea"), config.getString("owner"),
                    AreaFactory.MODE.valueOf(config.getString("mode").toUpperCase()));
        } catch (IOException e) {
            logger.error(e.getMessage());
            return 1;
        }


        //create the reference to the job area
        JobArea jobArea = null;
        try {
            jobArea = AreaFactory.createJobArea(
                    config.getString("jobArea"), config.getString("owner"),
                    AreaFactory.MODE.valueOf(config.getString("mode").toUpperCase()));
        } catch (IOException ioe) {
            logger.error(ioe);
            return 1;
        }
        //load plugin configurations
        Plugins plugins = null;
        try {
            plugins = new Plugins(false);
            plugins.addServerConf(config.getFile("pluginDir").getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            if (plugins.somePluginReportedErrors()) {
                System.err.println("Some plugins could not be loaded. See below for details. Aborting.");

                return (1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            return (1);
        }
        try {
            String queue = config.getString("queue");
            Submitter submitter = null;
            Actions actions = null;
            if (config.getString("mode").equalsIgnoreCase("remote")) {
                if (!jobArea.isLocal()) {
                    submitter = new RemoteSubmitter(plugins.getRegistry(), queue);
                    actions = new Actions(submitter, storageArea, jobArea, plugins.getRegistry());
                } else {
                    logger.error("Cannot use the remote submitter with a local job area");
                }

            } else if (config.getString("mode").equalsIgnoreCase("local")) {
                if (jobArea.isLocal()) {

                    submitter = new LocalSubmitter(plugins.getRegistry());
                    actions = new Actions(submitter, storageArea, jobArea, plugins.getRegistry());
                }
            }
            assert actions != null : "action cannot be null.";
            submitter.setSubmissionHostname(config.getString("artifact-server"));
            submitter.setRemoteArtifactRepositoryPath(config.getString("repository"));
            if (config.userSpecified("env-script")) {
                submitter.setEnvironmentScript(config.getFile("env-script").getAbsolutePath());
            }

            if (config.userSpecified("task")) {
                String token[] = config.getStringArray("task");
                String id = token[0];

                actions.submitTask(
                        id,
                        config.getStringArray("inputFilesets"));
            } else if (config.userSpecified("resource")) {

                String token[] = config.getStringArray("resource");
                String id = token[0];
                String version = token[1];
                actions.submitResourceInstall(id, version);

            } else
                logger.error("Cannot use the local submitter with a remote job area");

        } catch (Exception e) {
            logger.error("Failed to manage the requested action", e);
            return (1);

        }
        return 0;
    }


    /**
     * Loads the parameters configuration and rules
     *
     * @param args the command line arguments
     * @return the configuration
     */
    private static JSAPResult loadConfig(String[] args) {
        if (ClusterGateway.class.getResource("ClusterGateway.jsap") == null) {
            logger.fatal("unable to find the JSAP configuration file");
            System.err.println("unable to find the JSAP configuration file");
            return null;
        }
        JSAP jsap = null;
        try {
            jsap = new JSAP(ClusterGateway.class.getResource("ClusterGateway.jsap"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSAPException e) {
            e.printStackTrace();
            return null;

        }
        List<String> errors = new ArrayList<String>();
        JSAPResult config = jsap.parse(args);
        if (config.userSpecified("help") || hasError(config, errors)) {
            if (errors.size() > 0) {
                for (String error : errors)
                    System.err.println("Error: " + error);
            }
            for (java.util.Iterator errs = config.getErrorMessageIterator(); errs.hasNext(); ) {
                System.err.println("Error: " + errs.next());
            }
            System.err.println(jsap.getHelp());
            System.err.println();
            System.err.println("Usage: java " + ClusterGateway.class.getName());
            System.err.println("                " + jsap.getUsage());
            System.err.println();

        }
        return config;
    }

    private static boolean hasError(JSAPResult config, List<String> errors) {
        // First, check for validation errors found by JSAP:
        if (!config.success()) {
            return true;
        }
        if (config.getString("mode").equalsIgnoreCase("local"))
            return false;

        if (config.getString("mode").equalsIgnoreCase("remote")) {
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
