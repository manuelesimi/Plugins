package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import org.campagnelab.gobyweb.artifacts.ArtifactRepo;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;

import java.util.SortedSet;

/**
 * Simulator for artifacts' requests.
 *
 * @author manuele
 */
public class ArtifactInstallerSimulator {

    protected static void populateArtifactsOptions(ResourceConsumerConfig pluginConfig, SortedSet<Option> env) {
       if (pluginConfig.requires != null && pluginConfig.requires.size() >0) {
            for (Resource resource : pluginConfig.requires) {
                ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.id,
                        resource.versionAtLeast, resource.versionExactly, resource.versionAtMost);
                if (resourceConfig != null)
                    populateArtifactsOptions(resourceConfig, env);
            }
        }
    }

    protected static void populateArtifactsOptions(ResourceConfig resourceConfig, SortedSet<Option> env) {
        if (!resourceConfig.requires.isEmpty()) {
            // recursively simulate PB requests for resources required by this resource.
            for (Resource prerequisite : resourceConfig.requires) {
                ResourceConfig preResourceConfig = DependencyResolver.resolveResource(prerequisite.id,
                        prerequisite.versionAtLeast,
                        prerequisite.versionExactly, prerequisite.versionAtMost);
                populateArtifactsOptions(preResourceConfig, env);
            }
        }

        if (!resourceConfig.artifacts.isEmpty()) {
            for (Artifact artifactXml : resourceConfig.artifacts) {
                //add all attributes' values as variable
                addOptionWithAttributesValues(resourceConfig.getId(), artifactXml, env);
                //add each attribute name as variable
                ArtifactRepo repo = new ArtifactRepo(null);//just need the object to use the normalize method
                for (Attribute attribute : artifactXml.attributes) {
                    String key = String.format("RESOURCES_ARTIFACTS_%s_%s_%s",resourceConfig.getId(),artifactXml.id,
                        repo.normalize(attribute.name));
                    env.add(new Option(key, "", Option.OptionKind.STRING));
                }
            }
        }
    }

    private static void addOptionWithAttributesValues(String id, Artifact artifactXml, SortedSet<Option> env) {
        StringBuffer sb = new StringBuffer();
        for (Attribute attribute : artifactXml.attributes) {
            if ( (attribute.value != null) && (attribute.value.length() > 0)) {
                sb.append("_");
                sb.append(attribute.value);
            }
        }
        String key = String.format("RESOURCES_ARTIFACTS_%s_%s%s", id,artifactXml.id,
                sb.toString());
            env.add(new Option(key, "", Option.OptionKind.STRING));
    }
}
