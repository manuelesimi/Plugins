package org.campagnelab.gobyweb.clustergateway.jobs;

import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Simulate the environment that a job will see at execution time.
 *
 * @author manuele
 */
public class JobBuilderSimulator {

    private final ExecutableConfig executableConfig;

    private final ResourceConfig resourceConfig;

    private final PluginRegistry registry;

    /**
     *
     * @param config the plugin configuration from which the job would be created.
     * @param registry
     */
    public JobBuilderSimulator(ResourceConfig config, PluginRegistry registry) {
        this.resourceConfig = config;
        this.registry = registry;
        this.executableConfig = null;
    }

    /**
     *
     * @param config the plugin configuration from which the job would be created.
     * @param registry
     */
    public JobBuilderSimulator(ExecutableConfig config, PluginRegistry registry) {
        this.executableConfig = config;
        this.registry = registry;
        this.resourceConfig = null;
    }

    /**
     * Tries to recreate the job submission environment.
     * @return the list of variables visible to the job.
     * @throws IOException
     */
    public Map<String,String> simulateAutoOptions() throws IOException {
        Map<String,String> env = new ConcurrentSkipListMap<String,String>();
        env.clear();
        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);
        File autoOptionsFile;
        if (executableConfig !=null)
             autoOptionsFile = helper.generateAutoOptionsFile(executableConfig, null, null, null);
        else {
            Resource resource = new Resource();
            resource.id = this.resourceConfig.getId();
            resource.versionExactly = this.resourceConfig.getVersion();
            autoOptionsFile = File.createTempFile("auto-option", "");
            PrintWriter writer = new PrintWriter(autoOptionsFile);
            helper.writeResourceFileVariables(resource, writer);
            writer.close();
        }
        BufferedReader br = new BufferedReader(new FileReader(autoOptionsFile));
        try {
            String line = br.readLine();
            while (line != null) {
                this.parseLine(line,env);
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        FileUtils.deleteQuietly(autoOptionsFile);
        return env;
    }

    /**
     * Extracts the variable name from a line in the format NAME=VALUE.
     * @param line
     */
    private void parseLine(String line,Map env) {
        String[] tokens = line.split("=");
        if (tokens.length == 2)
            env.put(tokens[0],tokens[1]);
    }
}
