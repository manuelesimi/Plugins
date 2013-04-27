package org.campagnelab.gobyweb.clustergateway.jobs;


import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskIO;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskInputSchema;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * A task instance.
 *
 * @author manuele
 */

public class TaskJob extends ParametrizedJob {

    /**
     * Files belonging to the task that need to be copied on the cluster before its execution
     */
    private List<File> files = new ArrayList<File>();

    private TaskConfig sourceConfig;

    /**
     * Creates a new instance.
     *
     * @param sourceConfig the task configuration loaded from the plugin directory
     */
    public TaskJob(TaskConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
            for (PluginFile file : sourceConfig.getFiles()) {
                this.addFile(file.getLocalFile());
            }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getMandatoryInputSlots() {
        List<String> mandatorySlots = new ArrayList<String>();
        for (TaskIO schemaInputSlot : sourceConfig.getInputSchema().getInputSlots()) {
            String minOccurs = schemaInputSlot.geType().minOccurs;
            if ((minOccurs != null) || (Integer.valueOf(minOccurs) > 0))
                mandatorySlots.add(schemaInputSlot.getName());
        }
        return mandatorySlots;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getMandatoryOutputSlots() {
        List<String> mandatorySlots = new ArrayList<String>();
        for (TaskIO schemaOutputSlot : sourceConfig.getOutputSchema().getOutputSlots()) {
            String minOccurs = schemaOutputSlot.geType().minOccurs;
            if ((minOccurs != null) || (Integer.valueOf(minOccurs) > 0))
                mandatorySlots.add(schemaOutputSlot.getName());
        }
        return mandatorySlots;
    }

    /**
     * Validates the input value against the input schema of the task.
     * The validation checks if the schema defines an input slot with that name
     * and if the cardinality of its values matches the limits declared in the slot
     * definition.
     * @param value  the input slot value to check
     * @return true if valid, false otherwise
     */
    @Override
    protected boolean validateInputSlotValue(InputSlotValue value) {
        TaskInputSchema inputSchema = sourceConfig.getInputSchema();
        for (TaskIO schemaInputSlot : inputSchema.getInputSlots()) {
           if (schemaInputSlot.getName().equalsIgnoreCase(value.getName())) {
               //check the cardinality of the values
               List<String> actualValues = value.getValues();
               TaskIO.IOFileSetRef type = schemaInputSlot.geType();
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

    public TaskConfig getSourceConfig() {
        return this.sourceConfig;
    }

    /**
     * Gets the list of files belonging the instance.
     *
     * @return
     */
    public List<File> getFiles() {
        return this.files;
    }

    /**
     * Adds a new file belonging the instance. The file will be copied in the task execution folder.
     *
     * @param file
     */
    public void addFile(File file) {
        this.files.add(file);
    }

}
