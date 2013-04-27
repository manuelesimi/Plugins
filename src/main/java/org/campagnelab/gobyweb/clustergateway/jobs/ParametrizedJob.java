package org.campagnelab.gobyweb.clustergateway.jobs;

import java.util.*;

/**
 * A Job with input slots.
 *
 * @author manuele
 */
public abstract class ParametrizedJob extends Job {

     private Set<InputSlotValue> inputSlots = new HashSet<InputSlotValue>();

    /**
     * Adds a new actual value for an input slot
     * @param value
     * @throws org.campagnelab.gobyweb.clustergateway.jobs.ParametrizedJob.InvalidSlotValueException if the parameter is not valid
     */
    public void addInputSlotValue(InputSlotValue value) throws InvalidSlotValueException {
      //if (!(value.getValues().size()>0))
      //    throw new InvalidSlotValueException(String.format("Parameter %s has no value(s) " + value.toString()));
      //else
      if (! validateInputSlotValue(value))
          throw new InvalidSlotValueException(String.format("Input slot %s does not match the Job configuration", value.toString()));
      else
          inputSlots.add(value);
    }

    /**
     * Adds new actual values for the input slots.
     * @param values
     * @throws org.campagnelab.gobyweb.clustergateway.jobs.ParametrizedJob.InvalidSlotValueException if any of the values is not valid
     */
    public void addInputSlotValues(Set<InputSlotValue> values) throws InvalidSlotValueException {
        for (InputSlotValue value : values)
            this.addInputSlotValue(value);
    }

    /**
     *  Gets the values of the input slot
     *  @param slotName the name of the input slot
     *  @return
     */
    public List<String> getInputSlotValues(String slotName) {
        for (InputSlotValue inputSlotValue: inputSlots) {
            if (inputSlotValue.getName().equalsIgnoreCase(slotName))
                return inputSlotValue.getValues();
        }
        return Collections.EMPTY_LIST;
    }


    /**
     * Validates the input slot value according to the Job configuration.
     *
     * @param value
     * @return true if the value is accepted, false otherwise
     */
    protected abstract boolean validateInputSlotValue(InputSlotValue value);

    /**
     * Gets the list of mandatory input slots
     * @return the names of the slots
     */
    protected abstract List<String> getMandatoryInputSlots();

    /**
     * Gets the list of mandatory output slots
     * @return the names of the slots
     */
    protected abstract List<String> getMandatoryOutputSlots();


    public static class InvalidSlotValueException extends Exception {

        public InvalidSlotValueException(String message) {
            super(message);
        }
    }
}
