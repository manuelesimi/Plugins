package org.campagnelab.gobyweb.clustergateway.data;

import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;

import java.io.IOException;
import java.util.*;
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
     * @param file
     */
    public void addEntry(String name, File file) {
        this.entry2file.put(name, new Entry(name,file, FileUtils.sizeOf(file)));
    }

    /**
     * Gets the relative path to the entry file(s). The path is relative to the fileset basename.
     * @param name the name of the entry
     * @return
     * @throws IOException
     */
    public String getEntryPath(String name) throws IOException {
       if (this.entry2file.containsKey(name)) {
        return this.entry2file.get(name).file.getName();
       } else
           throw new IOException("the entry does not exist");
    }

    /**
     * Gets the entry file.
     * @param name  the name of the entry
     * @return
     * @throws IOException
     */
    public File getEntryFile(String name) throws IOException {
        if (this.entry2file.containsKey(name)) {
            return this.entry2file.get(name).file;
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
     * Checks if all the mandatory entries of the fileset have been assigned to files.
     */
    public boolean isComplete() {
        return (this.getMissingEntries(false).size()<=0);
    }

    /**
     * Gets the list of entries with no file(s) assigned.
     * @param includeOptionalEntries
     * @return
     */
    public List<String> getMissingEntries(boolean includeOptionalEntries) {
        List<String> entries = new ArrayList<String>();
        for (FileSetConfig.ComponentSelector entry : sourceConfig.getAllSelectors()) {
            if (!entry2file.containsKey(entry.getId())) {
                if (includeOptionalEntries) {
                    entries.add(entry.getId());
                } else { //return it only if it was mandatory
                    if (entry.getMandatory())
                        entries.add(entry.getId());
                }
            }
        }
        return entries;
    }

    /**
     * Checks if the entry match the fileset configuration.
     * @param name the entry name
     * @param file the entry file
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
     * Gets the source configuration.
     * @return
     */
    public FileSetConfig getSourceConfig() {
        return this.sourceConfig;
    }


    /**
     * An entry in the fileset
     */
    class Entry {
        String name;
        File file;
        long size;

        Entry(String name, File file, long size) {
            this.name = name;
            this.file = file;
            this.size = size;
        }
    }
}
