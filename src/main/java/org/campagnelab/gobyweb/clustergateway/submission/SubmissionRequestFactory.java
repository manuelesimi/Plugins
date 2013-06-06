package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.aligners.AlignerConfig;
import org.campagnelab.gobyweb.plugins.xml.alignmentanalyses.AlignmentAnalysisConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import java.io.File;

/**
 * A factory for submission requests.
 *
 * @author manuele
 */
public class SubmissionRequestFactory {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(SubmissionRequestFactory.class);

    public static SubmissionRequest createRequest(String[] args) throws Exception {

        //load plugin configurations
        Plugins plugins = null;
        plugins = new Plugins(false);
        plugins.addServerConf(getPluginsDir(args).getAbsolutePath());
        plugins.setWebServerHostname("localhost");
        plugins.reload();
        if (plugins.somePluginReportedErrors()) {
            throw new Exception("Some plugins could not be loaded. See below for details. Aborting.");
        }

        String[] pluginInfo = getPluginInfo(args);
        SubmissionRequest request = null;
        if (pluginInfo[0].equalsIgnoreCase("resource")) {
            request = new ResourceSubmissionRequest();
        } else if  (pluginInfo[0].equalsIgnoreCase("job")) {
            AlignerConfig alignerConfig = plugins.getRegistry().findByTypedId(pluginInfo[1], AlignerConfig.class);
            if (alignerConfig != null)
                request = new AlignerSubmissionRequest(alignerConfig);

            AlignmentAnalysisConfig analysisConfig = plugins.getRegistry().findByTypedId(pluginInfo[1], AlignmentAnalysisConfig.class);
            if (analysisConfig != null)
                request = new AlignmentAnalysisSubmissionRequest(analysisConfig);

            TaskConfig taskConfig = plugins.getRegistry().findByTypedId(pluginInfo[1], TaskConfig.class);
            if (taskConfig != null)
                request =  new TaskSubmissionRequest(taskConfig);
        }
        if (request != null) {
            request.setPlugins(plugins);
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
    private static String[] getPluginInfo(String[] args) {
        for (int index=0; index<args.length; index++)  {
            if (args[index].equalsIgnoreCase("--job")) {
                return new String[] {"job", args[++index]};
            }
            if (args[index].equalsIgnoreCase("--resource")) {
                return new String[] {"resource", args[++index]};
            }
        }
        //if we are here, no plugins dir has been found
        throw new IllegalArgumentException("Error: One parameter between '--job' and '--resource' is required.");

    }
}
