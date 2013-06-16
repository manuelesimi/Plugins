package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.InputSlotValue;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.Slot;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Base submission request for jobs.
 *
 * @author manuele
 */
public abstract class SubmissionRequest {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(SubmissionRequest.class);

    /**
     * The executable plugin that will be submitted
     * If subclasses set it, some submission information are extracted by the configuration.
     */
    protected ExecutableConfig executableConfig = null;

    protected static CommandLineHelper jsapHelper = new CommandLineHelper(ClusterGateway.class) {
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

    private Map<String, String> optionsMap = new HashMap<String, String>();

    private Set<InputSlotValue> inputSlots = Collections.EMPTY_SET;

    private Plugins plugins;

    private String[] commandLineArguments;

    protected Map<String, String> getUnclassifiedOptions() {
        return optionsMap;
    }

    protected Set<InputSlotValue> getInputSlots() {
        return inputSlots;
    }

    protected void setPlugins(Plugins plugins) {
        this.plugins = plugins;
    }

    /**
     * Sets the orginal command line arguments specified by the user.
     * @param commandLineArguments
     */
    protected void setCommandLineArguments(String[] commandLineArguments) {
        this.commandLineArguments = commandLineArguments;
    }

    /**
     * Builds the list of input slot parameters starting from the command line options
     *
     * @param parameters
     * @return
     * @throws Exception
     */
    //this method is static since some tests require it (not the best option)
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

    /**
     * Parses the additional options specified on the comman line and creates a map from them.
     * @param options option in the form KEY=VALUE,KEY2=VALUE2
     * @return
     */
    private void parseUnclassifiedOptions(String[] options) throws Exception {
        if (options == null)
            return;
        for (String inputAttribute: options) {
            String[] tokens = inputAttribute.split("=");
            if (tokens.length == 2) {
                optionsMap.put(tokens[0],tokens[1]);
            } else {
                logger.error("Invalid options format" + inputAttribute);
                throw new Exception("Invalid options format" + inputAttribute);
            }
        }
    }

    /**
     * Subclasses may override this method to provide additional options to the interface.
     * @return
     */
    protected List<Parameter> getAdditionalParameters() {
        return new ArrayList<Parameter>();
    }
    /**
     * Submits the request to the Cluster Gateway.
     * @return 0 if the request was successfully submitted, anything else if it failed
     * @throws Exception
     */
    protected int submitRequest() throws Exception {
        List<Parameter> additionalParameters = this.getAdditionalParameters();
        if (this.executableConfig != null) {
            //add parameters from plugin configuration
            for (org.campagnelab.gobyweb.plugins.xml.executables.Option option : executableConfig.getOptions().option){
                FlaggedOption jsapOption = new FlaggedOption(option.id)
                        .setStringParser(JSAP.STRING_PARSER)
                        .setRequired(option.required)
                        .setShortFlag(JSAP.NO_SHORTFLAG)
                        .setLongFlag(option.id)
                        .setDefault(option.defaultsTo);
                jsapOption.setHelp(option.help);
                additionalParameters.add(jsapOption);
            }
        }
        this.addInputSlots(additionalParameters);
        JSAPResult config = jsapHelper.configure(commandLineArguments, additionalParameters);
        if (config == null)
            return 1;

        //add the parameters to the options map
        for (Parameter parameter : additionalParameters) {
            if (! parameter.getID().equalsIgnoreCase("slots")) //we do not want the input slots in constants.sh
                this.optionsMap.put(parameter.getID(), config.getString(parameter.getID()));
        }
        String owner = config.userSpecified("owner") ? config.getString("owner") : System.getProperty("user.name");

        //create the reference to the job area
        JobArea jobArea = null;
        try {
            String jobAreaLocation = config.getString("job-area");

            jobArea = AreaFactory.createJobArea(
                    jobAreaLocation, owner);
        } catch (IOException ioe) {
            logger.error(ioe);
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
            if (config.userSpecified("env-script"))
                submitter.setEnvironmentScript(config.getFile("env-script").getAbsolutePath());

            if (config.userSpecified("option"))
                this.parseUnclassifiedOptions(config.getStringArray("option"));

            if (config.userSpecified("slots"))
                this.inputSlots = toInputParameters(config.getStringArray("slots"));

            return this.submit(config,actions);
        } catch (Exception e) {
            logger.error("Failed to manage the requested action", e);
            return (1);
        }

    }

    /**
     * Adds the input slot parameter to the interface according to the executable plugin configuration, if any.
     * @param additionalParameters
     */
    private void addInputSlots(List<Parameter> additionalParameters) {
        if ((this.executableConfig == null) || (executableConfig.getInput().getInputSlots().size() == 0))
            return;
        StringBuilder description = new StringBuilder("List of input slots for the job. ");
        description.append(String.format("%s accepts the following input slots: \n", this.executableConfig.getId()));
        StringBuilder formDescription = new StringBuilder();
        for (Slot slot: executableConfig.getInput().getInputSlots()){
            description.append(String.format("- %s (instance of %s): minOccurs %s, maxOccurs %s\n",slot.getName(),
                    slot.geType().id,slot.geType().minOccurs, slot.geType().maxOccurs));
            if (slot.geType().maxOccurs.equalsIgnoreCase("unbounded"))
                formDescription.append(String.format("%s: TAG1 ... TAGN ", slot.getName()));
            else if  (Integer.valueOf(slot.geType().maxOccurs) > 1)
                formDescription.append(String.format("%s: TAG1 ... TAG%s ", slot.getName(), slot.geType().maxOccurs));
            else
                formDescription.append(String.format("%s: TAG ", slot.getName()));
        }
        description.append(String.format("\nThe list must be provided in the format:\n%s", formDescription.toString()));
        UnflaggedOption option = new UnflaggedOption("slots").setRequired(true).setGreedy(true).setStringParser(JSAP.STRING_PARSER);
        option.setHelp(description.toString());
        additionalParameters.add(option);
    }

    protected abstract int submit(JSAPResult config, Actions actions) throws Exception;

}
