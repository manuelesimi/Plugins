package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAPResult;

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
        actions.submitResourceInstall(id, version);
        return 0;
    }
}
