package org.campagnelab.gobyweb.clustergateway.data;

import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.File;

/**
 * A fileset instance.
 *
 * @author manuele
 */
public class FileSet extends Job {

    private FileSetConfig sourceConfig;

    private Map<String, Entry> entry2file = new HashMap<String, Entry>();

    public FileSet() {}

    /**
     *
     * @param sourceConfig the configuration of this fileset loaded by the Plugins System
     */
    public FileSet(FileSetConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    /**
     * Adds an entry to the fileset.
     * @param name
     * @param filename
     * @param size
     */
    public void addEntry(String name, String filename, long size) throws IOException  {
        this.entry2file.put(name, new Entry(name,filename,size));
    }

    /**
     * Gets the relative path to the entry file(s). The path is relative to the fileset basename.
     * @param name
     * @return
     * @throws IOException
     */
    public String getEntryPath(String name) throws IOException {
       if (this.entry2file.containsKey(name)) {
        return this.entry2file.get(name).filename;
       } else
           throw new IOException("the entry does not exist");
    }

    /**
     * Gets the size of the file entry in bytes.
     * @param name
     * @return the size or zero if the entry does not exist
     */
    public Long getEntrySize(String name) {
        if (this.entry2file.containsKey(name))
         return this.entry2file.get(name).size;
            else
        return 0L;
    }

    /**
     * Gets the name of all the entries of the fileset.
     * @return
     */
    public Set<String> getAllEntryNames() {
        return Collections.unmodifiableSet(this.entry2file.keySet());
    }

    /**
     * Checks if the entry match the fileset configuration
     * @param name the entry name
     * @param file the entry file
     * TODO: to implement
     */
    public boolean validateEntry(String name, File file) {
        //validate entries
        for (FileSetConfig.ComponentSelector entry : sourceConfig.getFileSelectors()) {
           if (entry.getId().equalsIgnoreCase(name))
               return true; //TODO: more checks are needed here
        }
        for (FileSetConfig.ComponentSelector entry : sourceConfig.getDirSelectors()) {
            if (entry.getId().equalsIgnoreCase(name))
                return true; //TODO: more checks are needed here
        }
        return false;
    }


    /**
     * An entry in the fileset
     */
    class Entry {
        String name;
        String filename;
        long size;

        Entry(String name, String filename, long size) {
            this.name = name;
            this.filename = filename;
            this.size = size;
        }
    }
}
