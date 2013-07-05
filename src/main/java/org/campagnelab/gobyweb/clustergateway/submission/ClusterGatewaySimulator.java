package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.JobBuilderSimulator;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.Arrays;
import java.util.List;
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
            return false;
        }
    };

    public static void main(String[] args) {
        try {
            process(args);
            System.exit(0);
        } catch (Exception e) {
            logger.error("Failed to simulate the request.");
            System.exit(1);
        }
    }

    /**
     * Processes the caller request.
     * @param args
     * @return
     */
    public static void process(String[] args) throws Exception{
        JSAPResult config = jsapHelper.configure(args);
        if (config == null)
            System.exit(1);

        //load plugin configurations
        Plugins plugins = null;
        try {
            plugins = new Plugins();
            plugins.addServerConf(config.getFile("plugins-dir").getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            if (plugins.somePluginReportedErrors()) {
                throw new Exception("Some plugins could not be loaded.");
            }
        } catch (Exception e) {
            logger.error("Failed to load plugins definitions",e);
            throw new Exception("Failed to load plugins definitions");
        }
        JobBuilderSimulator builderSimulator = null;
        String[] pluginInfoData = null;
        if (config.userSpecified("job")) {
            pluginInfoData = config.getStringArray("job");
            ExecutableConfig executableConfig = plugins.getRegistry().findByTypedIdAndVersion(pluginInfoData[0],pluginInfoData[1], ExecutableConfig.class);
            if (executableConfig == null) {
                throw new Exception(String.format("Cannot find plugin configuration %s",Arrays.toString(pluginInfoData)));
            }
            builderSimulator = new JobBuilderSimulator(executableConfig,plugins.getRegistry());


        } if (config.userSpecified("resource")) {
            pluginInfoData = config.getStringArray("resource");
            ResourceConfig resourceConfig = plugins.getRegistry().findByTypedIdAndVersion(pluginInfoData[0],pluginInfoData[1], ResourceConfig.class);
            if (resourceConfig == null) {
                throw new Exception(String.format("Cannot find plugin configuration %s",Arrays.toString(pluginInfoData)));
            }
            builderSimulator = new JobBuilderSimulator(resourceConfig,plugins.getRegistry());
        }
        assert builderSimulator != null;
        assert pluginInfoData != null;
        SortedSet<String> env = builderSimulator.simulateAutoOptions();
        System.out.println(String.format("Plugin %s has access to the following environment variables:", Arrays.toString(pluginInfoData)));
        System.out.println("");
        for (String var : env) {
            System.out.println(var);
        }
        return;
    }
}
