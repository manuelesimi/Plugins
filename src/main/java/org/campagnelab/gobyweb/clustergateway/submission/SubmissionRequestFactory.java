package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.javatuples.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory for submission requests.
 *
 * @author manuele
 */
public class SubmissionRequestFactory {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(SubmissionRequestFactory.class);

    public static SubmissionRequest createRequest(String[] args, boolean fromAPI) throws Exception {

        //load plugin configurations
        PluginRegistry pluginRegistry = PluginRegistry.getRegistry();

        if (pluginRegistry.size() == 0 || !fromAPI) {
            logger.info("Loading available plugins...");
            Plugins plugins = null;
            plugins = new Plugins(false);
            plugins.addServerConf(getPluginsDir(args).getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            if (plugins.somePluginReportedErrors()) {
                throw new Exception("Some plugins could not be loaded. See below for details. Aborting.");
            }
        }

        List<Pair<String,String>> pluginsInfo = getPluginInfo(args);
        SubmissionRequest request = null;
        for (Pair<String,String> pluginInfo : pluginsInfo) {
            if (pluginInfo.getValue0().equalsIgnoreCase("resource")) {
                if (pluginInfo.getSize() == 1)
                    request = new ResourceSubmissionRequest(pluginInfo.getValue1());
            } else if  (pluginInfo.getValue0().equalsIgnoreCase("job")) {
                AlignerConfig alignerConfig = pluginRegistry.findByTypedId(pluginInfo.getValue1(), AlignerConfig.class);
                if (alignerConfig != null)
                    request = new AlignerSubmissionRequest(alignerConfig);

                AlignmentAnalysisConfig analysisConfig = pluginRegistry.findByTypedId(pluginInfo.getValue1(), AlignmentAnalysisConfig.class);
                if (analysisConfig != null)
                    request = new AlignmentAnalysisSubmissionRequest(analysisConfig);

                TaskConfig taskConfig = pluginRegistry.findByTypedId(pluginInfo.getValue1(), TaskConfig.class);
                if (taskConfig != null)
                    request =  new TaskSubmissionRequest(taskConfig);
            }
        }

        if (request != null) {
            request.setCommandLineArguments(args);
            return request;
        } else
            throw new Exception("Invalid plugin type.");
    }

    /**
     * Gets the plugins dir specified on the command line.
     * @return
     */
    private static File getPluginsDir(String[] args) {
        for (int index=0; index<args.length; index++)  {
            if ((args[index].equalsIgnoreCase("--plugins-dir") || (args[index].equalsIgnoreCase("-p")))) {
                return new File(args[++index]);
            }
        }
        //if we are here, no plugins dir has been found
        throw new IllegalArgumentException("Error: Parameter '--plugins-dir' is required.");
    }

    /**
     * Get the type and ID of the plugin to submit for execution.
     * @param args
     * @return an array of two elements: the first one is the type of plugin (job or resource), the second one is the plugin ID
     */
    private static List<Pair<String,String>> getPluginInfo(String[] args) {
        List<Pair<String,String>> pluginsInfo = new ArrayList<>();
        for (int index=0; index<args.length; index++)  {
            if (args[index].equalsIgnoreCase("--job")) {
                pluginsInfo.add(new Pair<String, String>("job",args[++index]));
            }
            if (args[index].equalsIgnoreCase("--resource")) {
                pluginsInfo.add(new Pair<String, String>("resource",args[++index]));
            }
        }
        if (pluginsInfo.size() > 0)
            return pluginsInfo;
        //if we are here, no plugins dir has been found
        throw new IllegalArgumentException("Error: One parameter between '--job' and '--resource' is required.");

    }
}
