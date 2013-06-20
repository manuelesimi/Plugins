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

    protected ResourceSubmissionRequest() {}

    @Override
    protected int submit(JSAPResult config, Actions actions) throws Exception {
        String token[] = config.getStringArray("resource");
        if (token.length == 0) {
            System.err.println("--resource argument must contain an ID.");
            System.exit(1);
        }
        String id = token[0];
        String version = null;
        if (token.length >= 2) {
            version = token[1];
        }
        ResourceConfig resourceConfig = DependencyResolver.resolveResource(id, version, version, version);
        if (resourceConfig.isDisabled())
            throw new Exception(String.format("Resource %s is currently disabled", resourceConfig.getId()));
        actions.submitResourceInstall(resourceConfig);
        return 0;
    }
}
