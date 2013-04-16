package org.campagnelab.gobyweb.clustergateway.data;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * Input parameter for a Job.
 *
 * @author manuele
 */
public final class InputParameter {

    private final String name;

    private final List<String> values = new ArrayList<String>();

    /**
     * Creates a new parameter with the given name
     * @param name
     */
    public InputParameter(String name) {
        this.name = name;
    }

    /**
     * Adds new values to the parameter.
     * @param values
     */
    public void addValues(String[] values) {
        this.values.addAll(Arrays.asList(values));
    }

    public List<String> getValues() {
        return values;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InputParameter that = (InputParameter) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "InputParameter{" +
                "name='" + name + '\'' +
                ", values=" + values +
                '}';
    }


}
