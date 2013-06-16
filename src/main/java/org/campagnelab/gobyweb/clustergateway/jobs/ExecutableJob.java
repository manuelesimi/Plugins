package org.campagnelab.gobyweb.clustergateway.jobs;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableInputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableOutputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.Slot;

import java.util.*;

/**
 * A Job with input slots.
 *
 * @author manuele
 */
public class ExecutableJob extends Job {

     private ExecutableConfig sourceConfig;

     private Set<InputSlotValue> inputSlots = new HashSet<InputSlotValue>();

     //a dynamic data object consumed by plugin scripts. At runtime, Groovy will resolve it with the actual object
     //the object stored in this variable depends on the job to be executed and it is prepared in the appropriate job builder.
     private Object dataForScripts;

     public ExecutableJob(ExecutableConfig sourceConfig) {
         this.sourceConfig = sourceConfig;
         for (PluginFile file : sourceConfig.getFiles()) {
             this.addFile(file.getLocalFile());
         }
     }

     public ExecutableOutputSchema getOutputSchema() {
        return sourceConfig.getOutput();
     }

     public ExecutableInputSchema getInputSchema() {
        return sourceConfig.getInput();
     }

    /**
     * Adds a new actual value for an input slot.
     * @param value
     * @throws ExecutableJob.InvalidSlotValueException if the parameter is not valid
     */
    public void addInputSlotValue(InputSlotValue value) throws InvalidSlotValueException {
      if (! validateInputSlotValue(value))
          throw new InvalidSlotValueException(String.format("Input slot %s does not match the Job configuration", value.toString()));
      else
          inputSlots.add(value);
    }

    /**
     * Adds new actual values for the input slots.
     * @param values
     * @throws ExecutableJob.InvalidSlotValueException if any of the values is not valid
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
     *
     * @return
     * @throws InvalidJobDataException if any of the mandatory slots is missing
     */
    public void validateMandatorySlots() throws InvalidJobDataException {
        List<String> mandatorySlots = this.getMandatoryInputSlots();
        List<String> inputSlotNames = new ArrayList<String>();
        for (InputSlotValue inputSlotValue : inputSlots)
            inputSlotNames.add(inputSlotValue.getName());
        //need to create two sets with the names to have a case insensitive comparison
        Set<String> mandatoryNameSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Set<String> actualNameSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        mandatoryNameSet.addAll(mandatorySlots);
        actualNameSet.addAll(inputSlotNames);

        if (!actualNameSet.containsAll(mandatoryNameSet)) {
            ObjectArraySet<String> missingNameSets = new ObjectArraySet<String>();
            missingNameSets.addAll(mandatoryNameSet);
            missingNameSets.removeAll(actualNameSet);
            throw new InvalidJobDataException("Some mandatory input slots are missing: " + missingNameSets.toString());
        }
    }


    /**
     * Validates the input value against the input schema of the task.
     * The validation checks if the schema defines an input slot with that name
     * and if the cardinality of its values matches the limits declared in the slot
     * definition.
     *
     * @param value the input slot value to check
     * @return true if the value is accepted, false otherwise
     */
    protected boolean validateInputSlotValue(InputSlotValue value) {
        ExecutableInputSchema inputSchema = sourceConfig.getInput();
        for (Slot schemaInputSlot : inputSchema.getInputSlots()) {
            if (schemaInputSlot.getName().equalsIgnoreCase(value.getName())) {
                //check the cardinality of the values
                List<String> actualValues = value.getValues();
                Slot.IOFileSetRef type = schemaInputSlot.geType();
                if (type.minOccurs !=null) {
                    if (Integer.valueOf(type.minOccurs) > actualValues.size()) {
                        logger.error(String.format("Input slot %s is not valid: at least %d values are expected (%d found)",
                                value.getName(), Integer.valueOf(type.minOccurs), actualValues.size()));
                        return false;
                    }
                }
                if (type.maxOccurs !=null) {
                    if (type.maxOccurs.equalsIgnoreCase("unbounded")
                            || (Integer.valueOf(type.maxOccurs) >= actualValues.size()))
                        return true;
                    else {
                        logger.error(String.format("Input slot %s is not valid: at most %d values are expected (%d found)",
                                value.getName(), Integer.valueOf(type.maxOccurs), actualValues.size()));
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the list of mandatory input slots
     *
     * @return the names of the slots
     */
    protected List<String> getMandatoryInputSlots() {
        List<String> mandatorySlots = new ArrayList<String>();
        for (Slot schemaInputSlot : sourceConfig.getInput().getInputSlots()) {
            String minOccurs = schemaInputSlot.geType().minOccurs;
            if ((minOccurs != null) || (Integer.valueOf(minOccurs) > 0))
                mandatorySlots.add(schemaInputSlot.getName());
        }
        return mandatorySlots;
    }

    /**
     * Gets the list of mandatory output slots
     *
     * @return the names of the slots
     */
    protected List<String> getMandatoryOutputSlots() {
        List<String> mandatorySlots = new ArrayList<String>();
        for (Slot schemaOutputSlot : sourceConfig.getOutput().getOutputSlots()) {
            String minOccurs = schemaOutputSlot.geType().minOccurs;
            if ((minOccurs != null) || (Integer.valueOf(minOccurs) > 0))
                mandatorySlots.add(schemaOutputSlot.getName());
        }
        return mandatorySlots;
    }

    /**
     * @return the source configuration of this Job
     */
    public ExecutableConfig getSourceConfig() {
        return sourceConfig;
    }

    public Object getDataForScripts() {
        return dataForScripts;
    }

    public void setDataForScripts(Object dataForScripts) {
        this.dataForScripts = dataForScripts;
    }

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
