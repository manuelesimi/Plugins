package org.campagnelab.gobyweb.clustergateway.jobs;

import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Simulate the environment that a job will see at execution time.
 *
 * @author manuele
 */
public class JobBuilderSimulator {

    private final ExecutableConfig executableConfig;

    private final PluginRegistry registry;

    private SortedSet<String> env = new TreeSet<String>();

    /**
     *
     * @param config the plugin configuration from which the job would be created.
     * @param registry
     */
    public JobBuilderSimulator(ExecutableConfig config, PluginRegistry registry) {
        this.executableConfig = config;
        this.registry = registry;
    }

    /**
     * Tries to recreate the job submission environment.
     * @return the list of variables visible to the job.
     * @throws IOException
     */
    public SortedSet<String> simulateAutoOptions() throws IOException {
        env.clear();
        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);
        File autoOptionsFile = helper.generateAutoOptionsFile(executableConfig, null, null, null);
        BufferedReader br = new BufferedReader(new FileReader(autoOptionsFile));
        try {
            String line = br.readLine();
            while (line != null) {
                this.parseLine(line);
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
    private void parseLine(String line) {
        String[] tokens = line.split("=");
        if (tokens.length == 2)
            this.env.add(tokens[0]);
    }
}
