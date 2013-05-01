package org.campagnelab.gobyweb.clustergateway.jobs;

import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableInputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableOutputSchema;

import java.util.*;

/**
 * A Job with input slots.
 *
 * @author manuele
 */
public abstract class ParametrizedJob extends Job {

     private Set<InputSlotValue> inputSlots = new HashSet<InputSlotValue>();

     public ExecutableOutputSchema getOutputSchema() {
        return this.getSourceConfig().getOutputSchema();
     }

     public ExecutableInputSchema getInputSchema() {
        return this.getSourceConfig().getInputSchema();
     }

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
     * Validate the job I/O available for its execution against the schema
     * @return
     * @throws InvalidJobDataException if any of the mandatory slots is missing
     */
    public void validateMandatorySlots() throws InvalidJobDataException {
        List<String> mandatorySlots = this.getMandatoryInputSlots();
        List<String> inputSlotNames = new ArrayList<String>();
        for (InputSlotValue inputSlotValue: inputSlots)
            inputSlotNames.add(inputSlotValue.getName());
        //need to create two sets with the names to have a case insensitive comparison
        Set <String> mandatoryNameSet = new TreeSet <String> (String.CASE_INSENSITIVE_ORDER);
        Set <String> actualNameSet = new TreeSet <String> (String.CASE_INSENSITIVE_ORDER);
        mandatoryNameSet.addAll(mandatorySlots);
        actualNameSet.addAll(inputSlotNames);

        if (!actualNameSet.containsAll(mandatoryNameSet))
              throw new InvalidJobDataException("Some mandatory input slots are missing");
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


    /**
     *
     * @return the plugin's source configuration of this Job
     */
    protected abstract ExecutableConfig getSourceConfig();


    public static class InvalidSlotValueException extends Exception {

        public InvalidSlotValueException(String message, Throwable throwable) {
            super(message, throwable);
        }

        public InvalidSlotValueException(String message) {
            super(message);
        }
    }

    public static class InvalidJobDataException extends Exception {

        public InvalidJobDataException(String message) {
            super(message);
        }

        public InvalidJobDataException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }
}
