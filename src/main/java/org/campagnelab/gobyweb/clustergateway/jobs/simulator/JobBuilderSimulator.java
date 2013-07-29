package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import static org.campagnelab.gobyweb.clustergateway.jobs.simulator.Option.*;

import java.io.*;
import java.util.SortedSet;
import java.util.TreeSet;

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
    public SortedSet<Option> simulateAutoOptions() throws IOException {
        SortedSet<Option> env = new TreeSet<Option>();
        if (executableConfig !=null)
            this.populateJobOptions(env);
        else  {
            Resource resource = new Resource();
            resource.id = this.resourceConfig.getId();
            resource.versionExactly = this.resourceConfig.getVersion();
            this.populateResourceOptions(resource, env);
        }
        return env;
    }


    private void populateResourceOptions(Resource resource, SortedSet<Option> env) throws IOException {
       ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.id,
                null, resource.versionExactly);
       if (resourceConfig == null)
           return;
       for (Resource ref : resourceConfig.requires)
           populateResourceOptions(ref, env);

        for (PluginFile file : resourceConfig.getFiles()) {
            String name = String.format("RESOURCES_%s_%s", resourceConfig.getId(), file.id);
            String value = String.format("${JOB_DIR}/%s",file.filename);
            env.add(new Option(name, value, file.isDirectory? OptionKind.DIRECTORY : OptionKind.FILE));
        }

    }

    private void populateJobOptions(SortedSet<Option> env) throws IOException {
        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);
        File autoOptionsFile = helper.generateAutoOptionsFile(executableConfig, null, null, null);
        //TODO to implement

        FileUtils.deleteQuietly(autoOptionsFile);

    }

}
