package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolve dependencies against the global singleton repository. See StatefulDependencyResolver to resolve against
 * an arbitrary PluginRepository.
 */
public class DependencyResolver {

    private static PluginRegistry pluginConfigs = PluginRegistry.getRegistry();
    private static StatefulDependencyResolver resolver= new StatefulDependencyResolver(pluginConfigs);


    /**
     * Return the resource with largest version number with the specified identifier.
     *
     * @param resourceId
     * @return Most recent resource (by version number) with id and version>v
     */
    public static ResourceConfig resolveResource(String resourceId) {
        return resolver.resolveResource(resourceId);
    }

    /**
     * Return the resource with largest version number, such that the resource has the identifier and at least the specified
     * version number.
     *
     * @param resourceId
     * @param versionAtLeast required version
     * @return Most recent resource (by version number) with id and version>v
     */
    public static ResourceConfig resolveResource(String resourceId, String versionAtLeast) {
        return resolver.resolveResource(resourceId, versionAtLeast, null, null);
    }

    /**
     * Return the resource with largest version number, such that the resource has the identifier and exactly the specified
     * version number.
     *
     * @param resourceId
     * @param versionAtLeast required version
     * @param versionExactly required exact this version
     * @return Most recent resource (by version number) with id and version>v
     */
    public static ResourceConfig resolveResource(String resourceId, String versionAtLeast, String versionExactly) {
        return resolver.resolveResource(resourceId, versionAtLeast, versionExactly, null);
    }

    public static FileSetConfig resolveFileSetFromMimeType(String mimeType) {
        return resolver.resolveFileSetFromMimeType(mimeType);
    }
    /**
     * Return the resource with largest version number, such that the resource has the identifier and at least the specified
     * version number.
     *
     * @param resourceId
     * @param versionAtLeast required version
     * @param versionExactly required exact this version
     * @param versionAtMost  max required version
     * @return Most recent resource (by version number) with id and version>v
     */
    public static ResourceConfig resolveResource(String resourceId, String versionAtLeast, String versionExactly, String versionAtMost) {
        return resolver.resolveResource(resourceId, versionAtLeast, versionExactly, versionAtMost);
    }
    /**
     * Return the fileset with largest version number, such that the resource has the identifier and at least the specified
     * version number.
     *
     * @param fileSetId
     * @param versionAtLeast required version
     * @param versionExactly required exact this version
     * @param versionAtMost  max required version
     * @return Most recent resource (by version number) with id and version>v
     */
    public static FileSetConfig resolveFileSet(String fileSetId, String versionAtLeast, String versionExactly, String... versionAtMost) {
        return resolver.resolveFileSet(fileSetId, versionAtLeast, versionExactly, versionAtMost);
    }
}
