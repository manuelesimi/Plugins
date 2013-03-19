package org.campagnelab.gobyweb.clustergateway.runtime;

import com.google.common.io.Files;
import edu.cornell.med.icb.net.CommandExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A local or remote area where files are persisted
 * @author manuele
 */
abstract class PersistentArea {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(PersistentArea.class);

    protected String path;

    protected CommandExecutor commandExecutor;

    protected boolean localArea = false;

    /**
     * Creates a new reference to the area.
     * @param referenceName the reference name of the area.
     * @param owner the owner of items persisted in the area
     * @throws IOException
     */
    protected PersistentArea(String referenceName, String owner) throws IOException {
        Pattern pattern = Pattern.compile("(.+?)@(.+?):(.+)"); //username@hostname:path
        Matcher matcher = pattern.matcher(referenceName);
        if (matcher.matches())  {
            //a remote area has been referred
            if (!remoteInitialize(matcher.group(2),matcher.group(1),matcher.group(3),owner))
                throw new IOException("Area initialization failed");
            this.path = matcher.group(3) + File.separator + owner;
        } else {
            //a local area has been referred
            File area = new File(referenceName + File.separator + owner);
            if (!localInitialize(area))
                throw new IOException("Area initialization failed");
            this.path = area.getAbsolutePath();
            this.localArea = true;
        }

    }

    /**
     * Returns whether this is a local or remote storage area.
     * @return true if this is a local area, false if this a remote area.
     */
    public boolean isLocal() {
        return localArea;
    }

    /**
     *
     * @return the path in the persistent area
     */
    public String getPath() {
        return path;
    }


    /**
     * Checks if the tag is already registered in the storage area
     * @param tag the tag
     * @return true if the tag is already in use
     */
    public boolean exists(String tag) {
        String destinationFolderName =  this.getBasename(tag);
        if (this.localArea) {
            //check the local folder existence
            File destination = new File(destinationFolderName);
            return (destination.exists() && destination.isDirectory())? true : false;
        } else {
            //check the remote folder existence
            try {
                return (commandExecutor.ssh(String.format("ls %s", destinationFolderName)) != 0 ) ? false: true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Creates the destination folder for the tag
     * @param tag
     * @return the path of the destination folder
     * @throws IOException
     */
    public String createTag(String tag) throws IOException {
        //check if the folder already exists
        if (this.exists(tag))
            throw new IOException(String.format("Tag=%s already exists in the target area",tag));
        //try to create the destination folder for the tag
        String destinationFolderName =  this.getBasename(tag);
        if (this.localArea) {
            FileUtils.forceMkdir(new File(destinationFolderName));
        } else {
            try {
                commandExecutor.ssh(String.format("mkdir %s", destinationFolderName));
            } catch (Exception e) {
                throw new IOException(String.format("Failed to create the destination folder for tag=%s. Reason: %s",
                        tag,e.getLocalizedMessage()));
            }
        }
        return destinationFolderName;
    }

    /**
     * Uploads a new file in the area.
     * @param tag the tag identifying the fileset instance
     * @param file the file to upload
     * @throws IOException if the upload fails
     */
    public void upload(String tag, File file) throws IOException {
        String destinationFolderName = this.getBasename(tag);
        File destinationFolder = new File(destinationFolderName);
        if (this.localArea) {
            //copy the file
            Files.copy(file, new File(destinationFolder, file.getName()));
        } else {
            //scp the file
            try {
                if (file.isDirectory())
                    commandExecutor.scpDirToRemote(file.getAbsolutePath(),destinationFolderName);
                else
                    commandExecutor.scpToRemote(file.getAbsolutePath(),destinationFolderName);
            } catch (InterruptedException e) {
                throw new IOException(String.format("Failed to upload file %s in the remote area. Reason: ",
                        file.getAbsolutePath(), e.getLocalizedMessage()));
            }
        }
    }

    /**
     * Gets the basename of the tag in the persistent area
     * @param tag
     * @return  the basename
     */
    public String getBasename(String tag) {
        return this.getPath() + File.separator + tag;
    }

    /**
     * Delete a tag from the area
     * @param tag the tag to remove
     */
    public void delete(String tag) throws IOException {
        String destinationFolderName =  this.getBasename(tag);
        File destinationFolder = new File(destinationFolderName);
        if (this.localArea) {
            try {
                FileUtils.deleteDirectory(destinationFolder);
            } catch (IOException e) {
                throw new IOException(String.format("Unable to delete tag at %s. Reason: %s ",
                        destinationFolder.getAbsolutePath(), e.getLocalizedMessage()));
            }
        } else {
            try {
                if (commandExecutor.ssh(String.format("rm -rf %s", destinationFolderName)) != 0 )
                    throw new IOException(String.format("Unable to delete tag at %s", destinationFolder));
            } catch (InterruptedException e) {
                throw new IOException(String.format("Unable to delete tag at %s. Reason: %s", destinationFolder, e.getLocalizedMessage()));
            }

        }
    }
    /**
     * Initializes a local persistent area
     * @param area the folder to use as local area
     * @return true if the initialization was correct, false otherwise
     */
    private boolean localInitialize(File area) {
        if (area.exists()) {
            if (area.isDirectory())
                return true;
            else {
                logger.error("the local area is not a directory");
                return false;
            }
        } else {
            //try to create it
            try {
                FileUtils.forceMkdir(area);
            } catch (IOException e) {
                logger.error("failed to create the local area",e);
                return false;
            }
        }
        return true;
    }

    /**
     * Initializes a remote persistent area
     * @param hostname
     * @param username
     * @param path
     * @param owner
     * @return true if the initialization was correct, false otherwise
     */
    private boolean remoteInitialize(String hostname, String username, String path, String owner) {
        this.commandExecutor  = new CommandExecutor(username, hostname);
        commandExecutor.setQuiet(true);
        try {
            if (commandExecutor.ssh(String.format("ls %s", path)) != 0) {
                logger.error("failed to access the remote area");
                return false;
            }
        } catch (Exception e) {
            logger.error("failed to access the remote area",e);
            return false;
        }
        //check the owner's folder and, if it does not exist, try to create it
        try {
            if ((commandExecutor.ssh(String.format("ls %s/%s", path, owner)) != 0)
                && (commandExecutor.ssh(String.format("mkdir %s/%s", path, owner)) != 0)) {
                    logger.error("failed to initialize the remote area");
                    return false;
            }
        } catch (Exception e) {
            logger.error("failed to initialize the remote area",e);
            return false;
        }
        return true;
    }

}
