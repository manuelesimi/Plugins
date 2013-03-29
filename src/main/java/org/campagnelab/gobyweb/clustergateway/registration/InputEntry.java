package org.campagnelab.gobyweb.clustergateway.registration;

import com.esotericsoftware.wildcard.Paths;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Access information about an input entry.
 * @author manuele
 */
class InputEntry {

    protected final org.apache.log4j.Logger logger = Logger.getLogger(InputEntry.class);

    private final String filesetConfigId;
    private final String pattern;
    private final List<InputEntryFile> files;

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
     * Checks if all the files of the entry have been consumed
     * @return
     */
    public boolean isConsumed() {
        for (InputEntryFile file : this.files) {
            if (!file.isConsumed())
                return false;
        }
        return true;
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
