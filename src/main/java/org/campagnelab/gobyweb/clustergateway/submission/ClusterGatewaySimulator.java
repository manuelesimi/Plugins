package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.simulator.JobBuilderSimulator;
import org.campagnelab.gobyweb.clustergateway.jobs.simulator.Option;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * This interface simulates actions performed during job submission.
 * Its scope is to produce useful information that can be used at submission time.
 *
 * @author manuele
 */
public class ClusterGatewaySimulator {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(ClusterGatewaySimulator.class);

    private static CommandLineHelper jsapHelper = new CommandLineHelper(ClusterGatewaySimulator.class) {
        @Override
        protected boolean hasError(JSAPResult config, List<String> errors) {
            if (! config.getString("action").equalsIgnoreCase("view-job-env")) {
                errors.add("Invalid action. 'view-job-env' has to be specified");
                return true;
            }
            if ((config.userSpecified("job") ? 1 : 0) + (config.userSpecified("resource") ? 1 : 0) > 1) {
                errors.add("Only one parameter between job and resource has to be specified");
                return true;
            }
            if ((config.userSpecified("job") ? 1 : 0) + (config.userSpecified("resource") ? 1 : 0) < 1) {
                errors.add("One parameter between job and resource has to be specified");
                return true;
            }

            return false;
        }
    };

    public static void main(String[] args) {
        try {
            SortedSet<Option> env = process(args, true);
            System.out.println("");
            for (Option option : env) {
                System.out.println(option.name);
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to simulate the request." + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Processes the caller request.
     * @param args
     * @return
     */
    public static SortedSet<Option> process(String[] args, boolean fromCommandLine) throws Exception{
        JSAPResult config = jsapHelper.configure(args);
        if (config == null) {
            if (fromCommandLine)
                System.exit(1);
            else
                throw new Exception("Invalid input parameters");
        }
        //load plugin configurations
        Plugins plugins = new Plugins();
        try {
            plugins.addServerConf(config.getFile("plugins-dir").getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            if (plugins.somePluginReportedErrors()) {
                throw new Exception(String.format("Some plugins could not be loaded. %s", plugins.getPluginReportedErrors()));
            }
        } catch (Exception e) {
            logger.error("Failed to load plugins definitions",e);
            throw new Exception("Failed to load plugins definitions." + plugins.getPluginReportedErrors());
        }
        JobBuilderSimulator builderSimulator = null;
        String[] pluginInfoData = null;
        if (config.userSpecified("job")) {
            pluginInfoData = parsePluginInfoData(config.getStringArray("job"));
            ExecutableConfig executableConfig = plugins.getRegistry().findByTypedIdAndVersion(pluginInfoData[0],pluginInfoData[1], ExecutableConfig.class);
            if (executableConfig == null) {
                throw new Exception(String.format("Cannot find plugin configuration %s",Arrays.toString(pluginInfoData)));
            }
            builderSimulator = new JobBuilderSimulator(executableConfig,plugins.getRegistry());
            pluginInfoData[1] = executableConfig.getVersion();

        } if (config.userSpecified("resource")) {
            pluginInfoData = parsePluginInfoData(config.getStringArray("resource"));
            ResourceConfig resourceConfig = plugins.getRegistry().findByTypedIdAndVersion(pluginInfoData[0],pluginInfoData[1], ResourceConfig.class);
            if (resourceConfig == null) {
                throw new Exception(String.format("Cannot find plugin configuration %s",Arrays.toString(pluginInfoData)));
            }
            builderSimulator = new JobBuilderSimulator(resourceConfig,plugins.getRegistry());
            pluginInfoData[1] = resourceConfig.getVersion();
        }
        assert builderSimulator != null;
        assert pluginInfoData != null;
        if (fromCommandLine)
            System.out.println(String.format("Plugin %s has access to the following environment variables:", Arrays.toString(pluginInfoData)));
        return  builderSimulator.simulateAutoOptions();
    }


    private static String[] parsePluginInfoData(String[] data) {
        String[] pluginInfoData = new String[2];
        pluginInfoData[0] = data[0];
        pluginInfoData[1] = (data.length == 2)? data[1] : null;
        return pluginInfoData;
    }
}
