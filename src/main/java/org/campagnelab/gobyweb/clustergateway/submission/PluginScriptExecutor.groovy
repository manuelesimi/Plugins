package org.campagnelab.gobyweb.clustergateway.submission

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.apache.log4j.Logger
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob
import org.campagnelab.gobyweb.plugins.xml.executables.Script

/**
 * Executor for plugin's scripts.
 *
 * @author manuele
 */
class PluginScriptExecutor {

    private static Logger logger = Logger.getLogger(PluginScriptExecutor.class);

    /**
     * Executes a plugin script. It requires that the script implements the method execute() and stores results in a given folder.
     * @param job
     * @param script
     * @param jobDir results of the plugin execution will be placed in this folder
     * @throws Exception if the script execution fails or the script is not expressed in a supported language
     * @return the folder were plugin script's results are placed
     */
    protected static void executeScript(ExecutableJob job, Script script, File jobDir) throws Exception {

        if (script.language == "groovy") {
            File returnedDir = executeGroovyScript(job, script);
            if (returnedDir != null && returnedDir.exists() && returnedDir.isDirectory()) {
                //copy the returned files in the job dir
                returnedDir.eachFile { File scriptOutputFile ->
                    File to = new File(jobDir, scriptOutputFile.getName());
                    Files.copy(scriptOutputFile, to);
                    if (scriptOutputFile.canExecute()) {
                        to.setExecutable(true);
                    }
                }
                FileUtils.deleteDirectory(returnedDir)
            } else {
                throw new RuntimeException("Error running script ${script.filename} for job ${job.getId()}: an invalid directory was returned");
            }

        } else {
            logger.error("Script filename ${script.filename} for job ${job.getId()} is in unsupported language ${script.language}") ;
            throw new IllegalArgumentException("Script filename ${script.filename} for job ${job.getId()} is in unsupported language ${script.language}");
        }
    }

    /**
     * Executes a groovy script.
     * @param job
     * @param script
     * @return the folder where the script results are placed
     * @throws RuntimeException
     */
    private static File executeGroovyScript(ExecutableJob job, Script script) throws Exception {
        final File pluginScriptFilename = new File(job.getSourceConfig().getDirectory(), script.filename);
        final File tempDir;
        if (pluginScriptFilename.exists()) {
            logger.debug("Executing script ${pluginScriptFilename.getAbsolutePath()}...")
            tempDir = Files.createTempDir();
            try {
                final GroovyShell shell = new GroovyShell()
                groovy.lang.Script pluginScript = shell.parse(pluginScriptFilename)
                final int returnValue = pluginScript.execute(job.getDataForScripts(), tempDir)
                if (returnValue != 0) {
                    FileUtils.deleteDirectory(tempDir)
                    logger.error("Error running script ${script.filename} for job ${job.getSourceConfig().getId()}, return value was ${returnValue}");
                    throw new RuntimeException("Error running script ${script.filename} for job ${job.getSourceConfig().getId()}, return value was ${returnValue}");
                }
            } catch (Exception e) {
                // Error running script, remove the tempDir
                FileUtils.deleteDirectory(tempDir)
                logger.error("Error running script ${script.filename} for job ${job.getSourceConfig().getId()}", e)
                throw new RuntimeException("Error running script ${script.filename} for job ${job.getSourceConfig().getId()}");
            }
        } else {
            logger.error("Script filename ${script.filename} for job ${job.getSourceConfig().getId()} doesn't exist")
            throw new IllegalArgumentException("Script filename ${script.filename} for job ${job.getSourceConfig().getId()} doesn't exist");
        }
        return tempDir;
    }
}
