package org.campagnelab.gobyweb.clustergateway.submission;

import com.martiansoftware.jsap.JSAPResult;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Prepare a job request for installing multiple resources.
 *
 * @author manuele
 */
class MultipleResourceSubmissionRequest extends SubmissionRequest {

    private final List<Pair<String,String>> resources;


    /**
     * @param resource the resource parameter coming from the command line.
     */
    protected MultipleResourceSubmissionRequest(String resource) {
        this.resources = new ArrayList<>();
        this.addResource(resource);
    }


    public void addResource(String resource) {
        String token[] = resource.split(":");
        Pair<String,String> r = new Pair<>(token[0], null);
        if (token.length >= 2) {
            r.setAt1(token[1]);
        }
        this.resources.add(r);
    }

    @Override
    protected int submit(JSAPResult config, Actions actions) throws Exception {
        List<ResourceConfig> configs = new ArrayList<>();
        for (Pair<String,String> resource : this.resources) {
            ResourceConfig resourceConfig = DependencyResolver.resolveResource(resource.getValue0(),
                    resource.getValue1(), resource.getValue1(), resource.getValue1());
            if (resourceConfig == null) {
                throw new IllegalArgumentException("Unable to resolve resource: " + resource.getValue0() + ":" + resource.getValue1());
            }
            if (resourceConfig.isDisabled())
                throw new Exception(String.format("Resource %s is currently disabled", resourceConfig.getId()));
            configs.add(resourceConfig);
        }
        actions.submitMultipleResourceInstall(configs, this.artifactsAttributes);
        return 0;
    }
}
