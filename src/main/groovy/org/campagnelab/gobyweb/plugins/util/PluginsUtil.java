package org.campagnelab.gobyweb.plugins.util;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.io.CommandLineHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.List;

/**
 * Implementation of the PluginsUtil interface
 *
 * @author manuele
 */
public class PluginsUtil {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(PluginsUtil.class);

    private static CommandLineHelper jsapHelper = new CommandLineHelper(PluginsUtil.class) {
        @Override
        protected boolean hasError(JSAPResult config, List<String> errors) {
            if (config.getString("action").equalsIgnoreCase("dependency-tree"))
                return false;
            else {
                errors.add("Invalid action " + config.getString("action"));
                return true;
            }
        }

    };

    public static void main(String[] args) {
        System.exit(process(args));
    }
    public static int process(String[] args) {
        JSAPResult config = jsapHelper.configure(args);
        if (config == null) return 1;
        //load plugin configurations
        Plugins plugins = null;
        try {
            plugins = new Plugins();
            plugins.addServerConf(config.getFile("plugins-dir").getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            if (plugins.somePluginReportedErrors()) {
                System.err.println("Some plugins could not be loaded. See below for details. Aborting.");
                throw new Exception();
            }
        } catch (Exception e) {
            logger.error("Failed to load plugins definitions",e);
            return 1;
        }

        String token[] = config.getStringArray("id");
        if (token.length == 0) {
            System.err.println("invalid argument: plugin must contain an ID.");
            return 1;
        }
        String id = token[0];
        String version = null;
        if (token.length >= 2) {
            version = token[1];
        }
        if (config.getString("action").equalsIgnoreCase("dependency-tree")) {
            DependencyTreeWriter treeWriter = new DependencyTreeWriter(plugins.getRegistry());
            ExecutableConfig executableConfig = PluginRegistry.getRegistry().findByTypedIdAndVersion(id, version, ExecutableConfig.class);
            if (executableConfig != null) {
                treeWriter.writeTree(System.out, executableConfig);
            } else {
                ResourceConfig resourceConfig = PluginRegistry.getRegistry().findByTypedIdAndVersion(id, version, ResourceConfig.class);
                if (resourceConfig != null)
                    treeWriter.writeTree(System.out,resourceConfig);
                else
                    System.err.println("invalid plugin: plugin must be of resource or executable type.");
            }

        }
        return 0;
    }


}
