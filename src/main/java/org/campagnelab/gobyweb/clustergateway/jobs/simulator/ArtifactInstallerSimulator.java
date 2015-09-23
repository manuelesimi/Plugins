package org.campagnelab.gobyweb.clustergateway.jobs.simulator;

import org.campagnelab.gobyweb.artifacts.ArtifactRepo;
import org.campagnelab.gobyweb.clustergateway.submission.SubmissionRequest;
import org.campagnelab.gobyweb.plugins.xml.common.Attribute;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Artifact;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;

import java.util.List;
import java.util.SortedSet;

/**
 * Simulator for artifacts' requests.
 *
 * @author manuele
 */
public class ArtifactInstallerSimulator {

    protected static void populateArtifactsOptions(ResourceConsumerConfig pluginConfig, SortedSet<Option> env,
                                                   SubmissionRequest.ArtifactInfoMap artifactsAttributes) {
       if (pluginConfig.requires != null && pluginConfig.requires.size() >0) {
            for (Resource resource : pluginConfig.requires) {
                ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.id,
                        resource.versionAtLeast, resource.versionExactly, resource.versionAtMost);
                if (resourceConfig != null)
                    populateArtifactsOptions(resourceConfig, env,artifactsAttributes);
            }
        }
    }

    protected static void populateArtifactsOptions(ResourceConfig resourceConfig, SortedSet<Option> env,
                                                   SubmissionRequest.ArtifactInfoMap artifactsAttributes) {
        if (!resourceConfig.requires.isEmpty()) {
            // recursively simulate PB requests for resources required by this resource.
            for (Resource prerequisite : resourceConfig.requires) {
                ResourceConfig preResourceConfig = DependencyResolver.resolveResource(prerequisite.id,
                        prerequisite.versionAtLeast,
                        prerequisite.versionExactly, prerequisite.versionAtMost);
                populateArtifactsOptions(preResourceConfig, env ,artifactsAttributes);
            }
        }

        if (!resourceConfig.artifacts.isEmpty()) {
            for (Artifact artifactXml : resourceConfig.artifacts) {
                //add var with the artifact name
                String attributeKey = String.format("RESOURCES_ARTIFACTS_%s_%s",resourceConfig.getId(),artifactXml.id);
                env.add(new Option(attributeKey, "", Option.OptionKind.STRING));
                //add all attributes' values as variable
                addOptionWithAttributesValues(resourceConfig.getId(), artifactXml, env, artifactsAttributes);
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

    private static void addOptionWithAttributesValues(String resourceId, Artifact artifactXml, SortedSet<Option> env,
                                                      SubmissionRequest.ArtifactInfoMap artifactsAttributes) {
        if (artifactsAttributes == null)
            return;
        StringBuffer sb = new StringBuffer();
        for (Attribute attribute : artifactXml.attributes) {
            List<SubmissionRequest.AttributeValuePair> values = artifactsAttributes.getAttributes(resourceId, artifactXml.id);
            if (values != null) {
                for (SubmissionRequest.AttributeValuePair value : values) {
                    if (value.name.equalsIgnoreCase(attribute.name)) {
                        sb.append("_");
                        sb.append(value.value.toUpperCase());
                    }
                }
            }
        }
        String key = String.format("RESOURCES_ARTIFACTS_%s_%s%s", resourceId, artifactXml.id, sb.toString());
        env.add(new Option(key, "", Option.OptionKind.STRING));
    }

}
