package org.campagnelab.gobyweb.clustergateway.submission;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Input parameter for a task.
 *
 * @author manuele
 */
class InputParameter {

    private final String name;

    private final List<String> values = new ArrayList<String>();

    /**
     * Creates a new parameter with the given name
     * @param name
     */
    protected InputParameter(String name) {
        this.name = name;
    }

    /**
     * Adds
     * new values to the parameter.
     * @param values
     */
    public void addValues(String[] values) {
        this.values.addAll(Arrays.asList(values));
    }
}
