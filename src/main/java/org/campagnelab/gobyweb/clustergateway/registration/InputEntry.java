package org.campagnelab.gobyweb.clustergateway.registration;

import com.esotericsoftware.wildcard.Paths;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An input entry specified by the caller.
 *
 * @author manuele
 */
class InputEntry {

    protected final org.apache.log4j.Logger logger = Logger.getLogger(InputEntry.class);

    private final String filesetConfigId;
    private final String pattern;
    private final List<InputEntryFile> files;
    private String fileSetEntryName;
    private ENTRY_TYPE fileSetEntryType;

    protected enum ENTRY_TYPE {
        FILE,DIR
    }

    /**
     * An entry with a fileset associated
     * @param filesetConfigId
     * @param pattern
     */
    protected InputEntry(String sourceDir, String filesetConfigId, String pattern) {
       this.filesetConfigId= filesetConfigId;
       this.pattern = pattern;
       files = new InputEntryScanner(sourceDir).scan();
    }

    /**
     * An entry without any fileset associated
     * @param pattern
     */
    protected InputEntry(String sourceDir, String pattern) {
        this(sourceDir,null,pattern);
    }

    /**
     * Gets the list of files belonging this entry
     * @return
     */
    protected List<InputEntryFile> getFiles() {
        return files;
    }

    protected boolean isBoundToFileSet() {
        if (filesetConfigId == null)
            return false;
        else
            return true;
    }

    protected String getFileSetId() {
        return this.filesetConfigId;
    }

    protected String getPattern() {
        return this.pattern;
    }

    /**
     * Records the fileset's entry name assigned to this input entry
     * @param name
     * @param type
     */
    protected void assignConfigEntry(String name, ENTRY_TYPE type) {
       this.fileSetEntryName = name;
       this.fileSetEntryType = type;
    }

    /**
     * Gets the assigned entry name.
     * @return
     */
    protected String getAssignedEntryName() {
        return this.fileSetEntryName;
    }

    /**
     * Gets the assigned entry type.
     * @return
     */
    public ENTRY_TYPE getFileSetEntryType() {
        return this.fileSetEntryType;
    }

    /**
     * Marks the whole entry as consumed.
     * After calling this method, the entry will not be further considered for
     * contributing to a fileset instance.
     */
    protected void markAsConsumed() {
        for (InputEntryFile file : this.files) {
            file.setConsumed(true);
        }
    }

    /**
     * Checks if all the files of the entry have been consumed
     * @return
     */
    public boolean hasNexFile() {
        for (InputEntryFile file : this.files) {
            if (!file.isConsumed())
                return true;
        }
        return false;
    }

    /**
     * Gets the next non consumed file
     * @return
     */
    public InputEntryFile nextFile() {
        for (InputEntryFile file : this.files) {
            if (!file.isConsumed())
                return file;
        }
        return null;
    }

    /**
     * Loads the list of files matching an entry pattern
     * @author manuele
     */
    class InputEntryScanner {

        private String dir;

        protected InputEntryScanner(String dir) {this.dir = dir;}

        /**
         * Gets the files matching the entry pattern.
         */
        private List<InputEntryFile> scan() {
            List<InputEntryFile> files = new ArrayList<InputEntryFile>();
            File workingDirectory = new File (dir);
            if (!acceptAsFile(files,workingDirectory)) {
                InputEntry.this.logger.debug("Scanning directory " + workingDirectory.getAbsolutePath());
                Paths paths = new Paths();
                paths.glob(workingDirectory.getAbsolutePath(), pattern);
                for (File file : paths.getFiles())
                    files.add(new InputEntryFile(file));
            }
            return files;
        }

        /**
         * Checks if the pattern is a valid filename.
         * @param inputFilesFile list to populate if the file is accepted
         * @param workingDirectory
         * @return
         */
        private boolean acceptAsFile(List<InputEntryFile> inputFilesFile, File workingDirectory)  {
            File file = new File(pattern);
            if (file.exists()) {
                inputFilesFile.add(new InputEntryFile(file));
                return true;
            }
            file = new File(workingDirectory, pattern);
            if (file.exists()) {
                inputFilesFile.add(new InputEntryFile(file));
                return true;
            }
            return false;
        }
    }
}
