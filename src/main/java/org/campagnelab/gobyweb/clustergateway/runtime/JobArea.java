package org.campagnelab.gobyweb.clustergateway.runtime;

import edu.cornell.med.icb.net.CommandExecutor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The area where job files are placed for execution.
 *
 * @author manuele
 */
public class JobArea extends PersistentArea {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(JobArea.class);

    /**
     * Creates a new reference to the job area.
     *
     * @param jobArea the job area parameter.
     * @param owner   the owner of the jobs stored in the area
     * @throws java.io.IOException
     */
    public JobArea(String jobArea, String owner) throws IOException {
        super(jobArea, owner);
    }

    /**
     * Gives execute permission to the specified files
     * @param tag the tag that identifies the job to which the files belong to
     * @param binaryFiles the list of files
     * @throws IOException
     */
    public void grantExecutePermissions(String tag, String[] binaryFiles) throws IOException {
        if (binaryFiles.length == 0)
            return;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < (binaryFiles.length - 1); i++)
            builder.append(String.format("chmod  +x %s/%s/%s;", this.getPath(), tag, binaryFiles[i]));
        builder.append(String.format("chmod  +x %s/%s/%s", this.getPath(), tag, binaryFiles[binaryFiles.length-1]));

        if (this.isLocal()) {
            Process p = Runtime.getRuntime().exec(builder.toString());
            if (p.exitValue() != 0) {
                logger.error("Failed to give execution permission to the task scripts");
                throw new RuntimeException("Failed to give execution rights to the task scripts");
            }
        } else {
            try {
                commandExecutor.ssh(builder.toString());
            } catch (InterruptedException e) {
                logger.error("Failed to give execution permission to the task scripts");
                throw new IOException("Failed to give execution permission to the task scripts");
            }
        }
    }

    /**
     * Executes the binary file in the job folder
     * @param tag the tag that identifies the job
     * @param binaryFile relative path to the file
     */
    public void execute(String tag, String binaryFile) throws Exception {
        if (this.isLocal()) {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(String.format("cd %s/%s; ./%s", this.getPath(), tag, binaryFile));
            } catch (Exception e) {
                logger.error("Failed to execute the local job script");
                throw e;
            }
            if (p.exitValue() != 0) {
                logger.error("Failed to execute the local job script. The script returned an error code.");
                throw new Exception("Failed to execute the local job script. The script returned an error code.");
            }
        } else {
            try {
                commandExecutor.ssh(String.format("cd %s/%s; ./%s", this.getPath(), tag, binaryFile));
            } catch (Exception e) {
                logger.error("Failed to execute the remote job script");
                throw e;
            }
        }

    }
}
