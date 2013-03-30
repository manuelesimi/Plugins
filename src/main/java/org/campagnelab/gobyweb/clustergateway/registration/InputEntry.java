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
    private InputEntryFile nextFile;

    /**
     * An entry with a fileset associated
     * @param filesetConfigId
     * @param pattern
     */
    protected InputEntry(String filesetConfigId, String pattern) {
       this.filesetConfigId= filesetConfigId;
       this.pattern = pattern;
       files = new InputEntryScanner().scan();
    }

    /**
     * An entry without any fileset associated
     * @param pattern
     */
    protected InputEntry(String pattern) {
        this.pattern = pattern;
        this.filesetConfigId = null;
        files = new InputEntryScanner().scan();
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
     * Marks the whole entry as consumed.
     * After calling this method, the entry will not be further considered for
     * contributing to a fileset instance.
     */
    public void markAsConsumed() {
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
                return false;
        }
        return true;
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

        protected InputEntryScanner() {}

        /**
         * Gets the files matching the entry pattern.
         */
        private List<InputEntryFile> scan() {
            List<InputEntryFile> files = new ArrayList<InputEntryFile>();
            File workingDirectory = new File (System.getProperty("user.dir"));
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
