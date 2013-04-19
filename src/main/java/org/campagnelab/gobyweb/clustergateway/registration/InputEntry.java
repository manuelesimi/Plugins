package org.campagnelab.gobyweb.clustergateway.registration;

import com.esotericsoftware.wildcard.Paths;
import org.apache.log4j.Logger;

import static org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
    private final File file;
    private final List<InputEntryFile> files;
    private String fileSetEntryName;
    /** The type of fileset entry to which this input entry was bound to*/
    private SELECTOR_TYPE fileSetEntryType;



    /**
     * An entry with a fileset associated
     * @param sourceDir the directory where to look for the entry files
     * @param filesetConfigId
     * @param pattern
     */
    protected InputEntry(String sourceDir, String filesetConfigId, String pattern) {
       this.filesetConfigId= filesetConfigId;
       File file =  new File(pattern);
       if (file.exists()) {
           this.file = file;
           this.pattern = null;
       } else {
           file = new File(new File (sourceDir), pattern);
           if (file.exists()) {
               this.file = file;
               this.pattern = null;
           }
           else {
               this.pattern = pattern;
               this.file = file;
           }
       }
       files = new InputEntryScanner(sourceDir).scan();
    }

    /**
     * An entry with no fileset associated.
     * @param sourceDir the directory where to look for the entry files
     * @param pattern
     */
    protected InputEntry(String sourceDir, String pattern) {
        this(sourceDir,null, pattern);
    }

    /**
     * Gets the list of files belonging this entry
     * @return
     */
    protected List<InputEntryFile> getFiles() {
        return Collections.unmodifiableList(files);
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

    protected File getFile() {
        return file;
    }

    /**
     * Gets a string with a human readable name for the entry
     * @return
     */
    protected String getHumanReadableName() {
        StringBuilder builder = new StringBuilder();
        if (this.isBoundToFileSet())
            builder.append(this.filesetConfigId).append(":");
        builder.append((this.getPattern()!=null)?this.getPattern():this.getFile().getName());
        return builder.toString();
    }
    /**
     * Records the fileset's entry name assigned to this input entry
     * @param name
     * @param type
     */
    protected void assignConfigEntry(String name, SELECTOR_TYPE type) {
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
    public SELECTOR_TYPE getFileSetEntryType() {
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
     * @return true if there is at least one more file to consume
     */
    public boolean hasNextFile() {
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
            if (!acceptAsFile(files)) {
                InputEntry.this.logger.debug("Scanning directory " + workingDirectory.getAbsolutePath());
                Paths paths = new Paths(); //see http://code.google.com/p/wildcard/
                paths.glob(workingDirectory.getAbsolutePath(), pattern);
                for (File file : paths.getFiles())
                    files.add(new InputEntryFile(file));
            }
            return files;
        }

        /**
         * Checks if the pattern is a valid filename.
         * @param inputFilesFile list to populate if the file is accepted
         * @return
         */
        private boolean acceptAsFile(List<InputEntryFile> inputFilesFile)  {
            File file = InputEntry.this.file;
            if (file.exists()) {
                inputFilesFile.add(new InputEntryFile(file));
                return true;
            }
            return false;
        }
    }
}
