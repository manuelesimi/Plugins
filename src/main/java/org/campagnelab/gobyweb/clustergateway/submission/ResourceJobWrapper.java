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

import java.util.List;
import java.util.Vector;

import static org.campagnelab.gobyweb.plugins.PluginLoaderSettings.SERVER_SIDE_TOOL;

/**
 * @author Fabien Campagne
 *         Date: 5/13/13
 *         Time: 5:20 PM
 */
public class ResourceJobWrapper extends ExecutableConfig {
    private ResourceConfig resource;

    @Override
    public String getId() {
        return resource.getId();
    }

    public ResourceJobWrapper(ResourceConfig sourceConfig) {
        super();
        this.resource = sourceConfig;
        this.id = resource.getId();
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
        return "Wrapping resource for job execution: " + resource;
    }

    @Override
    public String getHumanReadableConfigType() {
        return resource.getHumanReadableConfigType();
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
        return resource.getFiles();
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

        Resource thisResource = new Resource();
        thisResource.id = resource.getId();
        thisResource.versionAtLeast = resource.getVersion();
        thisResource.versionExactly = resource.getVersion();
        result.add(thisResource);


        result.addAll(resource.getRequiredResources());
        return result;
    }
}
