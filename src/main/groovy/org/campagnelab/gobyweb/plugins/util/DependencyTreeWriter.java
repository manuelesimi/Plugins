package org.campagnelab.gobyweb.plugins.util;

import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.io.PrintStream;

/**
 * Create dependency tree for plugins configurations
 *
 * @author manuele
 */

class DependencyTreeWriter {

    private PluginRegistry registry;

    protected DependencyTreeWriter(PluginRegistry registry) {
       this.registry = registry;
    }

    protected void writeTree(PrintStream stream, ExecutableConfig config) {
        stream.println(String.format("%s", config.toString()));
        for (Resource resource : config.getRequiredResources()) {
            writeResourceTree(stream,resource,"\t|");
        }
    }

    protected void writeTree(PrintStream stream, ResourceConfig config) {
        stream.println(String.format("%s", config.toString()));
        for (Resource resource : config.getRequiredResources()) {
            writeResourceTree(stream,resource,"\t|");
        }
    }

    private void writeResourceTree(PrintStream stream, Resource resource, String prefix) {
        ResourceConfig dependency = DependencyResolver.resolveResource(resource.id,
                resource.versionAtLeast, resource.versionExactly,
                resource.versionAtMost);
        if (dependency == null) {
            String message = String.format("Resource lookup failed for resource id=%s versionAtLeast=%s versionExactly=%s versionAtMost=%s%n.",
                    resource.id, resource.versionAtLeast, resource.versionExactly, resource.versionAtMost);
            stream.println(message);
            throw new RuntimeException(message);
        } else {
            stream.println(String.format("%s-%s", prefix,dependency.toString()));
            for (Resource requiredResource : dependency.getRequiredResources()) {
                writeResourceTree(stream, requiredResource, prefix + "\t|");
            }
        }

    }
}
