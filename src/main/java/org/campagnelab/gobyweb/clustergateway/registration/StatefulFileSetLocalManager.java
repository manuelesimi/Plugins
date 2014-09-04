package org.campagnelab.gobyweb.clustergateway.registration;

import org.apache.commons.io.IOUtils;
import org.campagnelab.gobyweb.filesets.FileSetAPI;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.filesets.registration.InputEntry;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A stateful version of the FileSetManager.
 *
 * @author manuele
 */
public class StatefulFileSetLocalManager extends BaseStatefulManager {

    private static final long serialVersionUID = 1526246795621236347L;

    public StatefulFileSetLocalManager(String filesetAreaReference, String owner) throws IOException {
       super(filesetAreaReference,owner);
    }



    /**
     * Registers a single instance fileset instance using the given type.
     *
     * @param fileSetID the type of fileset to register
     * @param paths list of pathnames matching the fileset entries
     * @param attributes
     * @param sharedWith
     * @param errors
     * @param tag
     * @return the list of tags of the newly registered filesets.
     * @throws Exception
     */
    @Override
    public List<String> register(String fileSetID,
                                 String[] paths, final Map<String, String> attributes,
                                 final List<String> sharedWith, List<String> errors, String tag) throws Exception {
        String[] entries = new String[paths.length + 1];
        entries[0] = fileSetID + ":";
        System.arraycopy(paths,0,entries,1,paths.length);
        if (configurationList.getConfiguration(fileSetID) == null) {
            errors.add("Unable to find a FileSet configuration with id=" + fileSetID);
            return Collections.emptyList();
        }
        FileSetAPI fileset = FileSetAPI.getReadWriteAPI(storageArea, configurationList);
        List<InputEntry> inputEntries = FileSetManager.parseInputEntries(entries);
        return fileset.register(inputEntries,attributes,sharedWith,errors,tag,null);
    }

    @Override
    public MetadataFileReader fetchMetadata(String tag, List<String> errors) throws IOException {
        FileSetAPI api = FileSetAPI.getReadOnlyAPI(storageArea);
        return api.fetchMetadata(tag, errors);
    }

    /**
     * Fetches an entry from the instance.
     *
     *
     * @param tag
     * @param errors
     * @return
     * @throws java.io.IOException
     */
    @Override
    public boolean fetchEntry(String entryName, String tag, List<String> paths, List<String> errors) throws IOException {
        FileSetAPI api = FileSetAPI.getReadOnlyAPI(storageArea);
        return api.fetchEntry(entryName,tag,paths,errors);
    }

    /**
     * Fetches an entry from the instance.
     *
     * @param entryName
     * @param tag
     * @param data
     * @param errors
     * @return
     * @throws java.io.IOException
     */
    @Override
    public boolean fetchStreamedEntry(String entryName, String tag, List<ByteBuffer> data, List<String> errors) throws IOException {
        FileSetAPI api = FileSetAPI.getReadOnlyAPI(storageArea);
        List<String> paths = new ArrayList<String>();
        if (api.fetchEntry(entryName,tag,paths,errors)) {
            if (data == null)
                data = new ArrayList<ByteBuffer>();
            for (String path : paths) {
                data.add(loadData(new File(path)));
            }
            return true;
        } else {
            return false;
        }
    }


    private ByteBuffer loadData(File file) {
        FileInputStream fIn;
        FileChannel fChan;
        long fSize;
        ByteBuffer mBuf;
        try {
            fIn = new FileInputStream(file);
            fChan = fIn.getChannel();
            fSize = fChan.size();
            mBuf = ByteBuffer.allocate((int) fSize);
            fChan.read(mBuf);
            mBuf.rewind();
            fChan.close();
            fIn.close();
            return mBuf;
        } catch (IOException exc) {
            return null;
        }
    }
}