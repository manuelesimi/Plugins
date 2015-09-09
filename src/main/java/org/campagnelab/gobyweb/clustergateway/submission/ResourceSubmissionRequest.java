package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAPResult;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

/**
 * Prepare a job request for a resource.
 *
 * @author manuele
 */
class ResourceSubmissionRequest extends SubmissionRequest {

    private String id;

    private String version;
    /**
     *
     * @param resource the resource parameter coming from the command line.
     */
    protected ResourceSubmissionRequest(String resource) {
        String token[] = resource.split(":");
        id = token[0];
        version = null;
        if (token.length >= 2) {
            version = token[1];
        }
    }

    @Override
    protected int submit(JSAPResult config, Actions actions) throws Exception {
        ResourceConfig resourceConfig = DependencyResolver.resolveResource(id, version, version, version);
        if (resourceConfig.isDisabled())
            throw new Exception(String.format("Resource %s is currently disabled", resourceConfig.getId()));
        actions.submitResourceInstall(resourceConfig, this.artifactsAttributes);
        return 0;
    }
}
