package org.campagnelab.gobyweb.clustergateway.data;

import java.util.HashSet;
import java.util.Set;

/**
 * A Job with input slots.
 *
 * @author manuele
 */
public abstract class ParametrizedJob extends Job {

    Set<InputSlotValue> parameters = new HashSet<InputSlotValue>();

    /**
     * Adds a new actual value for an input slot
     * @param value
     * @throws org.campagnelab.gobyweb.clustergateway.data.ParametrizedJob.InvalidInputSlotValueException if the parameter is not valid
     */
    public void addInputSlotValue(InputSlotValue value) throws InvalidInputSlotValueException {
      if (!(value.getValues().size()>0))
          throw new InvalidInputSlotValueException(String.format("Parameter %s has no value(s) " + value.toString()));
      else if (! validateInputSlotValue(value))
          throw new InvalidInputSlotValueException(String.format("Invalid parameter %s ", value.toString()));
      else
          parameters.add(value);
    }

    /**
     * Adds new actual values for the input slots.
     * @param values
     * @throws org.campagnelab.gobyweb.clustergateway.data.ParametrizedJob.InvalidInputSlotValueException if any of the values is not valid
     */
    public void addInputSlotValues(Set<InputSlotValue> values) throws InvalidInputSlotValueException {
        for (InputSlotValue value : values)
            this.addInputSlotValue(value);
    }

    /**
     *
     * @return
     */
    public Set<InputSlotValue> getParameters() {
        return parameters;
    }


    /**
     * Validates the input slot value according to the Job configuration.
     *
     * @param value
     * @return true if the value is accepted, false otherwise
     */
    protected abstract boolean validateInputSlotValue(InputSlotValue value);

    public static class InvalidInputSlotValueException extends Exception {

        public InvalidInputSlotValueException(String message) {
            super(message);
        }
    }
}
