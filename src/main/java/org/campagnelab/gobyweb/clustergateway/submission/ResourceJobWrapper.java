package org.campagnelab.gobyweb.clustergateway.submission;

import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableInputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableOutputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.Option;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.MERCURY;
import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.SERVER_SIDE_TOOL;

/**
 * @author Fabien Campagne
 *         Date: 5/13/13
 *         Time: 5:20 PM
 */
public class ResourceJobWrapper extends ExecutableConfig {
    private List<ResourceConfig> resources;

    @Override
    public String getId() {
        return resources.get(0).getId();
    }

    public ResourceJobWrapper(ResourceConfig sourceConfig) {
        super();
        this.resources = new ArrayList<>();
        this.resources.add(sourceConfig);
        this.id = resources.get(0).getId();
    }

    public ResourceJobWrapper(List<ResourceConfig> sourceConfigs) {
        super();
        this.resources = sourceConfigs;
        this.id = resources.get(0).getId();
    }

    @Override
    public ExecutableInputSchema getInput() {
        return new ExecutableInputSchema();
    }

    @Override
    public ExecutableOutputSchema getOutput() {
        return new ExecutableOutputSchema();
    }

    @Override
    public String toString() {
        return "Wrapping resource for job execution: " + resources.get(0);
    }

    @Override
    public String getHumanReadableConfigType() {
        return resources.get(0).getHumanReadableConfigType();
    }

    /**
     * Resources have no options.
     *
     * @return
     */
    public List<Option> options() {
        return new Vector<Option>();
    }

    @Override
    public List<PluginFile> getFiles() {
      List<PluginFile> files = new ArrayList<>();
      for (ResourceConfig r : resources)
        files.addAll(r.getFiles());
      return files;
    }

    @Override
    public List<Resource> getRequiredResources() {
        // include this resource in the list of resources to install:
        List<Resource> result = new Vector<Resource>();
        ResourceConfig serverSideResource = DependencyResolver.resolveResource(SERVER_SIDE_TOOL[0], SERVER_SIDE_TOOL[1], null);
        assert serverSideResource != null : " The ${SERVER_SIDE_TOOL[0]} resource must exist";
        Resource serverSideResourceRef = new Resource();
        serverSideResourceRef.id = serverSideResource.getId();
        serverSideResourceRef.versionExactly = serverSideResource.getVersion();
        result.add(serverSideResourceRef);

        ResourceConfig mercury = DependencyResolver.resolveResource(MERCURY[0], MERCURY[1], null);
        assert mercury != null : " The ${MERCURY[0]} resource must exist";
        Resource mercuryRef = new Resource();
        mercuryRef.id = mercury.getId();
        mercuryRef.versionExactly = mercury.getVersion();
        result.add(mercuryRef);

        for (ResourceConfig r : resources) {
            Resource thisResource = new Resource();
            thisResource.id = r.getId();
            thisResource.versionAtLeast = r.getVersion();
            thisResource.versionExactly = r.getVersion();
            result.add(thisResource);

            result.addAll(r.getRequiredResources());
        }

        return result;
    }
}
