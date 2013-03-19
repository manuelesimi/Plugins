package org.campagnelab.gobyweb.clustergateway.data;


import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.tasks.TaskConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;

/**
 * A task instance.
 *
 * @author manuele
 */

public class Task extends DataObject {

    /**
     * Tags that identify the input filesets
     */
    private List<String> fileSets = new ArrayList<String>();

    /**
     * Files belonging to the task that need to be copied on the cluster before its execution
     */
    private List<File> files = new ArrayList<File>();

    private TaskConfig sourceConfig;

    /**
     * Creates a new instance.
     * @param sourceConfig the task configuration loaded from the plugin directory
     */
    public Task(TaskConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
        for (PluginFile file : sourceConfig.getFiles()) {
            this.addFile(file.getLocalFile());
        }
    }

    /**
     * Add a new input fileset to the task instance.
     * @param fileset the tag referring the fileset
     */
    public void addInputFileSet(String fileset) {
        this.fileSets.add(fileset);
    }

    /**
     * Gets all the input filesets.
     * @return the list of tags
     */
    public List<String> getInputFileSets() {
        return Collections.unmodifiableList(this.fileSets);
    }

    public TaskConfig getSourceConfig() {
        return this.sourceConfig;
    }

    /**
     * Gets the list of files belonging the instance.
     * @return
     */
    public List<File> getFiles() {
        return this.files;
    }

    /**
     * Adds a new file belonging the instance. The file will be copied in the task execution folder.
     * @param file
     */
    public void addFile(File file) {
        this.files.add(file);
    }


}
