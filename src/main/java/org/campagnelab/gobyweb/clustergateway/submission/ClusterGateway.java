package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.InputSlotValue;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * Command line interface to the cluster gateway
 *
 * @author manuele
 */
public class ClusterGateway {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(ClusterGateway.class);

    private static CommandLineHelper jsapHelper = new CommandLineHelper(ClusterGateway.class) {
        @Override
        protected boolean hasError(JSAPResult config, List<String> errors) {
            boolean result = false;
            if ((config.userSpecified("resource") ? 1 : 0) + (config.userSpecified("job") ? 1 : 0) > 1) {
                errors.add("Only one parameter among resource and job has to be specified");
                result = true;
            }
            if ((config.userSpecified("resource") ? 1 : 0) + (config.userSpecified("job") ? 1 : 0) < 1) {
                errors.add("One parameter between resource and job has to be specified");
                result = true;
            }
            if (config.userSpecified("job-area")) {
                String jobAreadLocation = config.getString("job-area");
                if (jobAreadLocation.contains(":")) {
                    String[] tokens = jobAreadLocation.split(":");
                    if (tokens.length != 2) {
                        errors.add("remote job-area must contain two tokens separated by :. Second token was found missing: " + jobAreadLocation);
                        result = true;
                    } else {
                        jobAreadLocation = tokens[1];
                        if (!new File(jobAreadLocation).isAbsolute()) {
                            errors.add("--job-area argument must be an absolute path " + jobAreadLocation);
                            result = true;
                        }
                    }
                }
            }
            return result;
        }
    };

    public static void main(String[] args) {
        System.exit(process(args));
    }

    public static int process(String[] args) {
        JSAPResult config = jsapHelper.configure(args);
        if (config == null) return 1;
        String owner = config.userSpecified("owner") ? config.getString("owner") : System.getProperty("user.name");

        //create the reference to the job area
        JobArea jobArea = null;
        try {
            String jobAreadLocation = config.getString("job-area");

            jobArea = AreaFactory.createJobArea(
                    jobAreadLocation, owner);
        } catch (IOException ioe) {
            logger.error(ioe);
            return (1);
        }
        //load plugin configurations
        Plugins plugins = null;
        try {
            plugins = new Plugins(false);
            plugins.addServerConf(config.getFile("plugins-dir").getAbsolutePath());
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
                if ((config.userSpecified("job") && (config.userSpecified("queue"))))
                    submitter = new RemoteSubmitter(plugins.getRegistry(), config.getString("queue"));
                else if (config.userSpecified("resource"))
                    submitter = new RemoteSubmitter(plugins.getRegistry());
                else
                    throw new Exception("No queue has been indicated");
            }
            Actions actions = new Actions(submitter, config.getString("fileset-area"), jobArea, plugins.getRegistry());
            assert actions != null : "action cannot be null.";
            submitter.setSubmissionHostname(config.getString("artifact-server"));
            submitter.setRemoteArtifactRepositoryPath(config.getString("repository"));
            if (config.userSpecified("env-script")) {
                submitter.setEnvironmentScript(config.getFile("env-script").getAbsolutePath());
            }
            //request the appropriate action
            if (config.userSpecified("job"))
                requestJobSubmission(config, actions, plugins.getRegistry());
            else if (config.userSpecified("resource"))
                requestResourceSubmission(config, actions);
            else
                logger.error("Unrecognized or unspecified action.");

        } catch (Exception e) {
            logger.error("Failed to manage the requested action", e);
            return (1);

        }
        return 0;
    }

    private static void requestJobSubmission(JSAPResult config, Actions actions, PluginRegistry registry) throws Exception {
        String token[] = config.getStringArray("job");
        String id = token[0];
        Map<String, String> unclassifiedOptions = Collections.EMPTY_MAP;
        if (config.userSpecified("option"))
            unclassifiedOptions = parseUnclassifiedOptions(config.getStringArray("option"));
        AlignerConfig alignerConfig = registry.findByTypedId(id, AlignerConfig.class);
        if (alignerConfig != null) {
            if (!config.userSpecified("genome-reference-id"))
                throw  new IllegalArgumentException("Missing parameter genome-reference-id");
            if (!config.userSpecified("chunk-size"))
                throw  new IllegalArgumentException("Missing parameter chunk-size");
            if (!config.userSpecified("number-of-align-parts"))
                throw  new IllegalArgumentException("Missing parameter number-of-align-parts");

            //TODO: check, read and validate options from aligner config
            actions.submitAligner(id,
                    toInputParameters(config.getStringArray("slots")),
                    config.getString("genome-reference-id"),
                    config.getInt("chunk-size"),
                    config.getInt("number-of-align-parts"),
                    unclassifiedOptions
            );
        } else {
            actions.submitTask(id, toInputParameters(config.getStringArray("slots")),
                    unclassifiedOptions);
        }
    }

    /**
     * Parses the additional options specified on the comman line and creates a map from them.
     * @param options option in the form KEY=VALUE,KEY2=VALUE2
     * @return
     */
    public static Map<String, String> parseUnclassifiedOptions(String[] options) throws Exception {
        if (options == null)
            return Collections.emptyMap();
        Map<String, String> optionsMap = new HashMap<String, String>();
        for (String inputAttribute: options) {
            String[] tokens = inputAttribute.split("=");
            if (tokens.length == 2) {
                optionsMap.put(tokens[0],tokens[1]);
            } else {
                logger.error("Invalid options format" + inputAttribute);
                throw new Exception();
            }
        }
        return optionsMap;
    }


    /**
     * Requests the submission of a resource.
     * @param config
     * @param actions
     * @throws Exception
     */
    private static void requestResourceSubmission(JSAPResult config, Actions actions) throws Exception {
        String token[] = config.getStringArray("resource");
        if (token.length == 0) {
            System.err.println("--resource argument must contain an ID.");
            System.exit(1);
        }
        String id = token[0];
        String version = null;
        if (token.length >= 2) {
            version = token[1];
        }
        actions.submitResourceInstall(id, version);
    }


    /**
     * Builds the list of parameters starting from the command line input
     *
     * @param parameters
     * @return
     * @throws Exception
     */
    public static Set<InputSlotValue> toInputParameters(String[] parameters) throws Exception {
        if (parameters.length == 0)
            return Collections.EMPTY_SET;
        Set<InputSlotValue> parsed = new HashSet<InputSlotValue>();
        InputSlotValue param = null;
        if (parameters[0].endsWith(":"))
            param = new InputSlotValue(StringUtils.strip(parameters[0], ":"));
        else
            throw new Exception(String.format("Cannot accept tag reference %s with no parameter name associated. Accepted form is: NAME: TAG1 TAG2 NAME2: TAG3 TAG4 TAG5", parameters[0]));

        for (int i = 1; i < parameters.length; i++) {
            if (parameters[i].endsWith(":")) {
                //move to the new parameter
                parsed.add(param);
                param = new InputSlotValue(StringUtils.strip(parameters[i], ":"));
            } else
                param.addValues(parameters[i].split(","));
        }
        //add the last one
        parsed.add(param);
        return Collections.unmodifiableSet(parsed);
    }
}
