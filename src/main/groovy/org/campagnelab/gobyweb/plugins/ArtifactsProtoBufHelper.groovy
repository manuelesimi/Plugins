package org.campagnelab.gobyweb.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.artifacts.Artifacts;
import org.campagnelab.gobyweb.artifacts.BuildArtifactRequest
import org.campagnelab.gobyweb.plugins.xml.Config
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Fabien Campagne
 *         Date: 3/21/13
 *         Time: 11:24 AM
 */
public class ArtifactsProtoBufHelper {

    public static final String sshPattern = "(.+?)@(.+?):(.+)"; //username@hostname:path

    private static Logger LOG = Logger.getLogger(Plugins.class);
    private ObjectArrayList<String> pluginEnvironmentCollectionScripts = new ObjectArrayList<String>()
    private String webServerHostname;
    private String remotePluginsDir;
    private String localPluginsDir;

    private boolean dualRepoEnabled = false;

    public final String ARTIFACTS_INSTALL_REQUESTS = "artifacts-install-requests"

    void setWebServerHostname(String webServerHostname, String ... localPluginsDir) throws IOException {
        Pattern pattern = Pattern.compile(sshPattern); //username@hostname:path
        Matcher matcher = pattern.matcher(webServerHostname);
        if (matcher.matches())  {
            this.remotePluginsDir = matcher.group(3);
            this.webServerHostname = String.format("%s@%s", matcher.group(1), matcher.group(2));
            this.dualRepoEnabled = true;
            if (localPluginsDir == null || localPluginsDir.length !=1) {
                throw new IOException("A local plugins directory must be specified in order to build correct artifact paths.")
            } else {
                this.localPluginsDir = localPluginsDir[0];
            }

        } else {
            this.webServerHostname = webServerHostname
        }


    }
/**
 * Register environment collection scripts.
 * @param script
 */
    void registerPluginEnvironmentCollectionScript(String script) {
        this.pluginEnvironmentCollectionScripts.add(this.replaceIfDualRepo(script));
    }
    /**
     * Create artifacts install requests for the plugin given as argument. Traverse the graph of resource
     * dependency and order resource artifact installation such that resources that must be installed before
     * others are so. The client is responsible for deleting the result file when it is no longer needed.
     * @param pluginConfig Aligner, AlignmentAnalysis or Task config that uses resources.
     * @return null if the plugin does not require any artifacts, or a unique ile containing pb requests.
     */
    public File createPbRequestFile(ResourceConsumerConfig pluginConfig) {
        BuildArtifactRequest requestBuilder = this.createArtifactBuilder();
        def uniqueFile = File.createTempFile(ARTIFACTS_INSTALL_REQUESTS, ".pb");
        Set<String> alreadyInstalled=new HashSet<String>();
        buildPbRequest(requestBuilder, pluginConfig, alreadyInstalled)

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
     * @return null if the plugin does not require any artifacts, or a unique file containing pb requests.
     */
    public File createPbRequestFile(ResourceConfig resourceConfig) {
        List<ResourceConfig> configs = new ArrayList<>();
        configs.add(resourceConfig);
        return this.createPbRequestFileForMultipleResources(configs);

        /*BuildArtifactRequest requestBuilder = this.createArtifactBuilder();
        def uniqueFile = File.createTempFile(ARTIFACTS_INSTALL_REQUESTS, ".pb");
        Set<String> alreadyInstalled=new HashSet<String>();
        buildPbRequest(requestBuilder, resourceConfig, alreadyInstalled)
        // the resource has artifacts, so we write these to the PB request:
        buildPbRequest(resourceConfig, requestBuilder,alreadyInstalled, true)
        if (!requestBuilder.isEmpty()) {
            requestBuilder.save(uniqueFile);
            LOG.debug(requestBuilder.toString());
            return uniqueFile

        } else {
            return null;
        }*/
    }


    /**
     * Create artifacts install requests for the plugins given as argument. Traverse the graph of resource
     * dependency and order resource artifact installation such that resources that must be installed before
     * others are so. The client is responsible for deleting the result file when it is no longer needed.
     * @param resourceConfigs Resources that have artifacts.
     * @return null if the plugin does not require any artifacts, or a unique file containing pb requests.
     */
    public File createPbRequestFileForMultipleResources(List<ResourceConfig> resourceConfigs) {
        BuildArtifactRequest requestBuilder = this.createArtifactBuilder();
        def uniqueFile = File.createTempFile(ARTIFACTS_INSTALL_REQUESTS, ".pb");
        Set<String> alreadyInstalled=new HashSet<String>();
        for (ResourceConfig config : resourceConfigs) {
            buildPbRequest(requestBuilder, config, alreadyInstalled)
            // the resource has artifacts, so we write these to the PB request:
            buildPbRequest(config, requestBuilder,alreadyInstalled, true)
        }
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
    private void buildPbRequest(BuildArtifactRequest requestBuilder, ResourceConsumerConfig resourceConsumerConfig,
                                Set<String> alreadyInstalled) {
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
                buildPbRequest(resourceConfig, requestBuilder, alreadyInstalled, resource.mandatory)
        }


    }
    /**
     * Write PB artifact requests for a resource, starting with the artifacts of the resources required by the argument
     * resource.
     * @param resourceConfig
     * @param requestBuilder
     */
    private def buildPbRequest(ResourceConfig resourceConfig, BuildArtifactRequest requestBuilder,
                               Set<String> alreadyInstalled,
                               boolean mandatory = false) {
        LOG.debug("writePbForResource for " + resourceConfig?.id + " visiting resource dependencies..")
        if (!resourceConfig.requires.isEmpty()) {
            // recursively generate PB requests for resources required by this resource.
            for (Resource prerequisite : resourceConfig.requires) {
                ResourceConfig preResourceConfig = DependencyResolver.resolveResource(prerequisite.id,
                        prerequisite.versionAtLeast,
                        prerequisite.versionExactly, prerequisite.versionAtMost)
                buildPbRequest(preResourceConfig, requestBuilder, alreadyInstalled, mandatory)
                //if the source resource is mandatory, also its deps are mandatory.
            }

        }
        LOG.debug("writePbForResource for " + resourceConfig?.id + " writing artifact requests.")

        if (!resourceConfig.artifacts.isEmpty()) {
            // resource has artifacts. Generate the "install-requests.pb" file to tell the cluster nodes
            // how to install each artifact:

            String scriptFilename = resourceConfig.files.find { f -> f.id == "INSTALL" }.localFilename
            scriptFilename = replaceIfDualRepo(scriptFilename);
            for (Artifact artifactXml : resourceConfig.artifacts) {
                LOG.debug(String.format("PB request.add(%s:%s)", resourceConfig.id, artifactXml.id))
                // generate a unique key for each plugin and artifact:
                String key = "${resourceConfig.id}:${artifactXml.id}:${resourceConfig.version}"
                if (!alreadyInstalled.contains(key)) {
                    requestBuilder.addArtifactWithList(resourceConfig.id, artifactXml.id, resourceConfig.version, mandatory,
                            scriptFilename, Artifacts.RetentionPolicy.REMOVE_OLDEST, constructAvp(artifactXml))
                    alreadyInstalled.add(key);
                }

            }
        }
        LOG.debug("writePbForResource for " + resourceConfig?.id + " done.")

    }

    private String replaceIfDualRepo(String pluginFile) {
        if (dualRepoEnabled) {
           return pluginFile.replaceFirst(this.localPluginsDir, this.remotePluginsDir)
        }  else {
           return pluginFile;
        }
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

    private BuildArtifactRequest createArtifactBuilder() {
        return this.webServerHostname ? new BuildArtifactRequest(webServerHostname) : new BuildArtifactRequest();
    }

}
