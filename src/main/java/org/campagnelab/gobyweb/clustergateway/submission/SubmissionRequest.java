package org.campagnelab.gobyweb.clustergateway.submission;

import org.campagnelab.gobyweb.clustergateway.jobs.InputSlotValue;

import java.util.Map;
import java.util.Set;

/**
 * Base submission request for jobs.
 *
 * @author manuele
 */
abstract class SubmissionRequest {

    private Map<String, String> unclassifiedOptions;
    private Set<InputSlotValue> inputSlots;

    protected void setUnclassifiedOptions(Map<String, String> unclassifiedOptions) {
        this.unclassifiedOptions = unclassifiedOptions;
    }

    protected Map<String, String> getUnclassifiedOptions() {
        return unclassifiedOptions;
    }

    protected void setInputSlots(Set<InputSlotValue> inputSlots) {
        this.inputSlots = inputSlots;
    }

    protected Set<InputSlotValue> getInputSlots() {
        return inputSlots;
    }

    /**
     * Submits the request to the Cluster Gateway.
     * @param args
     * @param actions
     * @return
     * @throws Exception
     */
    protected abstract int submitRequest(String[] args, Actions actions) throws Exception;
}
