package org.campagnelab.gobyweb.plugins;

import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class DependencyResolver {

    private static PluginRegistry pluginConfigs = PluginRegistry.getRegistry();

    /**
      * Return the resource with largest version number with the specified identifier.
      *
      * @param resourceId
      * @return Most recent resource (by version number) with id and version>v
      */
     public static ResourceConfig resolveResource(String resourceId) {
         return resolveResource(resourceId, null, null, null);
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
        return resolveResource(resourceId, versionAtLeast, null, null);
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
        return resolveResource(resourceId, versionAtLeast, versionExactly, null);
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
        List<ResourceConfig> resourceList = new ArrayList<ResourceConfig>();
        synchronized (pluginConfigs) {
            for (ResourceConfig resource : pluginConfigs.filterConfigs(ResourceConfig.class)) {
                if (!resource.getId().equalsIgnoreCase(resourceId)) continue;

                if (versionExactly != null && !resource.exactlyVersion(versionExactly)) {
                    continue;
                }
                if (versionAtLeast != null && !resource.atLeastVersion(versionAtLeast)) {
                    continue;
                }
                if (versionAtMost != null && !resource.atMostVersion(versionAtMost)) {
                    continue;
                }

                resourceList.add(resource);

            }
        }
        if (resourceList.size() > 0) {
            Collections.sort(resourceList);
            return resourceList.get(0);
        } else
            return null;
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
        List<FileSetConfig> fileSetList = new ArrayList<FileSetConfig>();
        boolean checkAtMost = false;
        if (versionAtMost != null && versionAtMost.length > 0 && versionAtMost[0] != null) {
            checkAtMost = true;
        }
        synchronized (pluginConfigs) {
            for (FileSetConfig fileSet : pluginConfigs.filterConfigs(FileSetConfig.class)) {
                if (versionExactly != null) { //check exactly
                    if (fileSet.getId().equalsIgnoreCase(fileSetId) &&
                            fileSet.exactlyVersion(versionExactly))
                        fileSetList.add(fileSet);
                } else if (versionAtLeast != null) {
                    if (fileSet.getId().equalsIgnoreCase(fileSetId) &&
                            fileSet.atLeastVersion(versionAtLeast)) {
                        if (checkAtMost) { //check atLeast and atMost
                            if (fileSet.atMostVersion(versionAtMost[0])) {
                                fileSetList.add(fileSet);
                            }
                        } else  //accept because there is not atMost to check
                            fileSetList.add(fileSet);
                    }
                } else if (checkAtMost && (fileSet.getId().equalsIgnoreCase(fileSetId))) { //check only atMost
                    if (fileSet.atMostVersion(versionAtMost[0])) {
                        fileSetList.add(fileSet);
                    }
                }
            }
        }
        if (fileSetList.size() > 0) {
            Collections.sort(fileSetList);
            return fileSetList.get(0);
        } else
            return null;

    }

    public static FileSetConfig resolveFileSetFromMimeType(String mimeType) {
        //look for matching fileset configurations
        List<FileSetConfig> matchingFileSets = new ArrayList<FileSetConfig>();
        synchronized (pluginConfigs) {
            for (FileSetConfig fileSet : pluginConfigs.filterConfigs(FileSetConfig.class)) {
                if (fileSet.getMimeType().equalsIgnoreCase(mimeType)) {
                   matchingFileSets.add(fileSet);
                }
            }
        }
        //pick up the matching fileset with largest version number
        if (matchingFileSets.size() > 0) {
            Collections.sort(matchingFileSets);
            return matchingFileSets.get(0);
        } else
            return null;

    }

}
