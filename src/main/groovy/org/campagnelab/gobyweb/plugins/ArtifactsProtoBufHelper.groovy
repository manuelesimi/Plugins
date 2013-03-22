package org.campagnelab.gobyweb.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.Artifacts;
import org.campagnelab.gobyweb.artifacts.BuildArtifactRequest
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig

/**
 * @author Fabien Campagne
 *         Date: 3/21/13
 *         Time: 11:24 AM
 */
public class ArtifactsProtoBufHelper {


    private static Logger LOG = Logger.getLogger(Plugins.class);
    private ObjectArrayList<String> pluginEnvironmentCollectionScripts = new ObjectArrayList<String>()
    private webServerHostname;

    public final String ARTIFACTS_INSTALL_REQUESTS = "artifacts-install-requests"

    void setWebServerHostname(def webServerHostname) {
        this.webServerHostname = webServerHostname
    }
/**
 * Register environment collection scripts.
 * @param script
 */
    void registerPluginEnvironmentCollectionScript(String script) {
        this.pluginEnvironmentCollectionScripts.add(script);
    }
    /**
     * Create artifacts install requests for the plugin given as argument. Traverse the graph of resource
     * dependency and order resource artifact installation such that resources that must be installed before
     * others are so. The client is responsible for deleting the result file when it is no longer needed.
     * @param pluginConfig Aligner, AlignmentAnalysis or Task config that uses resources.
     * @return null if the plugin does not require any artifacts, or a unique ile containing pb requests.
     */
    public File createPbRequestFile(ResourceConsumerConfig pluginConfig) {
        BuildArtifactRequest requestBuilder = new BuildArtifactRequest(webServerHostname)
        def uniqueFile = File.createTempFile(ARTIFACTS_INSTALL_REQUESTS, ".pb");

        buildPbRequest(requestBuilder, pluginConfig)

        if (!requestBuilder.isEmpty()) {
            requestBuilder.save(uniqueFile);
            LOG.debug(requestBuilder.toString());
            return uniqueFile

        } else {
            return null;
        }
    }

    /**
     * Create artifacts install requests for the plugin given as argument. Traverse the graph of resource
     * dependency and order resource artifact installation such that resources that must be installed before
     * others are so. The client is responsible for deleting the result file when it is no longer needed.
     * @param resourceConfig A resource that has artifacts.
     * @return null if the plugin does not require any artifacts, or a unique ile containing pb requests.
     */
    public File createPbRequestFile(ResourceConfig resourceConfig) {
        BuildArtifactRequest requestBuilder = new BuildArtifactRequest(webServerHostname)
        def uniqueFile = File.createTempFile(ARTIFACTS_INSTALL_REQUESTS, ".pb");

        buildPbRequest(requestBuilder, resourceConfig)
        // the resource has artifacts, so we write these to the PB request:
        buildPbRequest(resourceConfig, requestBuilder)

        if (!requestBuilder.isEmpty()) {
            requestBuilder.save(uniqueFile);
            LOG.debug(requestBuilder.toString());
            return uniqueFile

        } else {
            return null;
        }
    }
    /**
     * Create artifacts install requests for the plugin given as argument. Traverse the graph of resource
     * dependency and order resource artifact installation such that resources that must be installed before
     * others are so. The client is responsible for deleting the result file when it is no longer needed.
     * @param pluginConfig
     * @return null if the plugin does not require any artifacts, or a unique ile containing pb requests.
     */
    private void buildPbRequest(BuildArtifactRequest requestBuilder, ResourceConsumerConfig resourceConsumerConfig) {
        LOG.debug("createPbRequestFile for " + resourceConsumerConfig?.id)

        pluginEnvironmentCollectionScripts.each { envScript ->
            requestBuilder.registerEnvironmentCollection(envScript)
        }
        // Create a single .pb file containing all resources that the plugin requires:
        // Each .pb file will contain the artifacts needed by the resource, starting with the artifacts that the
        // resource requires (deep first search)
        resourceConsumerConfig?.requires?.each {
            resource ->
                def resourceConfig = DependencyResolver.resolveResource(resource.id, resource.versionAtLeast, resource.versionExactly,
                        resource.versionAtMost)
                buildPbRequest(resourceConfig, requestBuilder)
        }


    }
    /**
     * Write PB artifact requests for a resource, starting with the artifacts of the resources required by the argument
     * resource.
     * @param resourceConfig
     * @param requestBuilder
     */
    private def buildPbRequest(ResourceConfig resourceConfig, BuildArtifactRequest requestBuilder) {
        LOG.debug("writePbForResource for " + resourceConfig?.id + " visiting resource dependencies..")
        if (!resourceConfig.requires.isEmpty()) {
            // recursively generate PB requests for resources required by this resource.
            for (Resource prerequisite : resourceConfig.requires) {
                ResourceConfig preResourceConfig = DependencyResolver.resolveResource(prerequisite.id,
                        prerequisite.versionAtLeast,
                        prerequisite.versionExactly, prerequisite.versionAtMost)
                buildPbRequest(preResourceConfig, requestBuilder)
            }

        }
        LOG.debug("writePbForResource for " + resourceConfig?.id + " writing artifact requests.")

        if (!resourceConfig.artifacts.isEmpty()) {
            // resource has artifacts. Generate the "install-requests.pb" file to tell the cluster nodes
            // how to install each artifact:

            String scriptFilename = resourceConfig.files.find { f -> f.id == "INSTALL" }.localFilename

            for (Artifact artifactXml : resourceConfig.artifacts) {
                LOG.debug(String.format("PB request.add(%s:%s)", resourceConfig.id, artifactXml.id))
                requestBuilder.addArtifactWithList(resourceConfig.id, artifactXml.id, resourceConfig.version,
                        scriptFilename, Artifacts.RetentionPolicy.REMOVE_OLDEST, constructAvp(artifactXml)
                )
            }
        }
        LOG.debug("writePbForResource for " + resourceConfig?.id + " done.")

    }


    static List<Artifacts.AttributeValuePair> constructAvp(Artifact artifact) {

        return artifact.attributes.collect {

            it ->
                def builder = Artifacts.AttributeValuePair.newBuilder().setName(it.name)
                if (it.value) {
                    builder.setValue(it.value)
                }
                builder.build()
        }

    }
}
