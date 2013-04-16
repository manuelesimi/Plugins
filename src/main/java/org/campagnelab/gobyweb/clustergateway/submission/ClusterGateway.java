package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.InputParameter;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.io.JobArea;

import java.io.IOException;
import java.util.*;


/**
 * Command line interface to the cluster gateway
 *
 * @author manuele
 */
public class ClusterGateway {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(ClusterGateway.class);
    // TODO : introduce CommandLineHelper after fileset registration_with_patterns branch has been merged back
    // TODO : into trunk.


    public static void main(String[] args) {
        System.exit(process(args));

    }

    public static int process(String[] args) {
        JSAPResult config = loadConfig(args);
        if (config == null) return 1;
        String owner = config.userSpecified("owner") ? config.getString("owner") : System.getProperty("user.name");
        //create the reference to the storage area
        FileSetArea storageArea = null;
        try {
            storageArea = AreaFactory.createFileSetArea(
                    config.getString("fileSetArea"), owner);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return 1;
        }


        //create the reference to the job area
        JobArea jobArea = null;
        try {
            jobArea = AreaFactory.createJobArea(
                    config.getString("jobArea"), owner);
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
            Submitter submitter = null;
            if (jobArea.isLocal()) {
                submitter = new LocalSubmitter(plugins.getRegistry());
            } else {
                if (!config.userSpecified("queue")) {
                    throw new Exception("No queue has been indicated");
                }
                submitter = new RemoteSubmitter(plugins.getRegistry(), config.getString("queue"));
            }
            Actions actions = new Actions(submitter, storageArea, jobArea, plugins.getRegistry());

            assert actions != null : "action cannot be null.";
            submitter.setSubmissionHostname(config.getString("artifact-server"));
            submitter.setRemoteArtifactRepositoryPath(config.getString("repository"));
            if (config.userSpecified("env-script")) {
                submitter.setEnvironmentScript(config.getFile("env-script").getAbsolutePath());
            }

            if (config.userSpecified("task")) {
                String token[] = config.getStringArray("task");
                String id = token[0];
                //toInputParameters(config.getStringArray("parameters"));
                actions.submitTask(
                        id, toInputParameters(config.getStringArray("parameters")));
            } else if (config.userSpecified("resource")) {

                String token[] = config.getStringArray("resource");
                String id = token[0];
                String version = token[1];
                actions.submitResourceInstall(id, version);

            } else
                logger.error("Unrecognized or unspecified action.");

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
        if (config.userSpecified("help") || (!config.success())) {
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

    /**
     * Builds the list of parameters starting from the command line input
     * @param parameters
     * @return
     * @throws Exception
     */
    private static  Set<InputParameter> toInputParameters(String[] parameters) throws Exception {
        Set<InputParameter> parsed = new HashSet<InputParameter>();
        InputParameter param = null;
        if (parameters[0].endsWith(":"))
            param = new InputParameter(StringUtils.strip(parameters[0], ":"));
        else
            throw new Exception("Cannot accept tag reference %s with no parameter name associated. Accepted form is: NAME: TAG1 TAG2 NAME2: TAG3 TAG4 TAG5");

        for (int i=1; i<parameters.length; i++) {
            if (parameters[i].endsWith(":")) {
                //move to the new parameter
                parsed.add(param);
                param = new InputParameter(StringUtils.strip(parameters[i], ":"));
            } else
                param.addValues(parameters[i].split(","));
        }
        //add the last one
        parsed.add(param);
        return Collections.unmodifiableSet(parsed);
    }
}
