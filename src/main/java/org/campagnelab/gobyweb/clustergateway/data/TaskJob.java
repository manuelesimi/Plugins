package org.campagnelab.gobyweb.clustergateway.data;


import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskIO;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskInputSchema;

import java.util.ArrayList;
import java.util.Collections;
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
     * Validates the parameter against the input schema of the task.
     * The validation checks if the schema defines a parameter with that name.
     * @param parameter
     * @return
     */
    @Override
    protected boolean validateParameter(InputParameter parameter) {
        TaskInputSchema inputSchema = sourceConfig.getInputSchema();
        for (TaskIO schemaParameter : inputSchema.getParameters()) {
           if (schemaParameter.getName().equalsIgnoreCase(parameter.getName()))
               return true;
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
