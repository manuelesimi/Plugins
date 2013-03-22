package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.data.Job;
import org.campagnelab.gobyweb.clustergateway.data.ResourceJob;
import org.campagnelab.gobyweb.clustergateway.data.TaskJob;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;
import org.campagnelab.gobyweb.plugins.ArtifactsProtoBufHelper;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConsumerConfig;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * @author Fabien Campagne
 *         Date: 3/21/13
 *         Time: 10:27 AM
 */
abstract public class AbstractSubmitter implements Submitter {

    protected PluginRegistry registry;
    protected String environmentScriptFilename;
    protected String artifactRepositoryPath;

    private static Logger logger = Logger.getLogger(Submitter.class);

    public void setSubmissionHostname(String submissionHostname) {
        this.submissionHostname = submissionHostname;
    }

    @Override
    public void setEnvironmentScript(String environmentScriptFilename) {
        this.environmentScriptFilename = environmentScriptFilename;
    }

    @Override
    public void setRemoteArtifactRepositoryPath(String artifactRepositoryPath) {
        assert artifactRepositoryPath != null : "artifactRepositoryPath cannot be null";
        this.artifactRepositoryPath = artifactRepositoryPath;
    }

    private String submissionHostname;

    protected AbstractSubmitter(PluginRegistry registry) {
        this.registry = registry;
    }

    @Override
    public abstract Session newSession();

    @Override
    public abstract void submitTask(JobArea jobArea, Session session, TaskJob taskJob) throws Exception;

    @Override
    public abstract void submitResourceInstall(JobArea jobArea, Session session, ResourceJob resource) throws Exception;

    /**
     * Collect resource files for a resource and its dependencies.
     *
     * @param resourceConfig The resource config for which files are sought.
     * @return List of plugin files.
     */
    public PluginFile[] collectResourceFiles(ResourceConfig resourceConfig) {
        ObjectArrayList<PluginFile> result = new ObjectArrayList<PluginFile>();
        collectResourceFiles(result, resourceConfig);
        return result.toArray(new PluginFile[result.size()]);
    }

    /**
     * Copy resource files to a destination directory. Handles directories appropriately.
     *
     * @param executableConfig
     * @param tempDir
     * @throws IOException
     */
    protected void copyResourceFiles(ExecutableConfig executableConfig, File tempDir) throws IOException {
        // copy all the resources' files in the local working dir
        for (PluginFile file : collectResourceFiles(executableConfig)) {
            if (file.isDirectory) {
                FileUtils.copyDirectory(file.getLocalFile(), new File(tempDir, file.getLocalFile().getName()));
            } else {
                Files.copy(file.getLocalFile(), new File(tempDir, file.getLocalFile().getName()));
            }
        }
    }

    protected void copyAutoOptions(ExecutableConfig executableConfig, File tempDir) throws IOException {
        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);
        File autoOptionsFile = helper.generateAutoOptionsFile(executableConfig);
        Files.copy(autoOptionsFile, new File(FilenameUtils.concat(tempDir.getAbsolutePath(), "auto-options.sh")));
    }

    /**
     * Generate the artifacts PB request file and copy to destination directory.
     *
     * @param executableConfig
     * @param envScriptFilename
     * @param tempDir
     * @throws IOException
     */
    protected void copyArtifactsPbRequests(ResourceConfig executableConfig, String envScriptFilename, File tempDir) throws IOException {
        ArtifactsProtoBufHelper helper = new ArtifactsProtoBufHelper();
        if (envScriptFilename != null) {
            helper.registerPluginEnvironmentCollectionScript(envScriptFilename);
        }
        assert submissionHostname != null : "submission hostname must be defined.";
        helper.setWebServerHostname(submissionHostname);
        File helperPbRequestFile = helper.createPbRequestFile(executableConfig);
        if (helperPbRequestFile != null) {
            Files.copy(helperPbRequestFile, new File(FilenameUtils.concat(tempDir.getAbsolutePath(), "artifacts-install-requests.pb")));
        }
    }


    /**
     * Copy resource files to a destination directory. Handles directories appropriately.
     *
     * @param resourceConfig
     * @param tempDir
     * @throws IOException
     */
    protected void copyResourceFiles(ResourceConfig resourceConfig, File tempDir) throws IOException {
        // copy all the resources' files in the local working dir
        for (PluginFile file : collectResourceFiles(resourceConfig)) {
            if (file.isDirectory) {
                File destDir = new File(tempDir, file.getLocalFile().getName());
                FileUtils.copyDirectory(file.getLocalFile(), destDir);
                // we do not set executable in files under dir.
            } else {
                File to = new File(tempDir, file.getLocalFile().getName());
                Files.copy(file.getLocalFile(), to);
                if (file.getLocalFile().canExecute()) {
                    logger.trace(String.format("Marking %s with executable flag.", to.getAbsolutePath()));
                    to.setExecutable(true);
                }
            }
        }
    }

    /**
     * Collect resource files for an executable plugin, including files for its resource dependencies.
     * Note that files with ID="INSTALL" are not collected because they are transmitted by the artifacts deployment
     * mechanism.
     *
     * @param pluginConfig The executable config for which files are sought.
     * @return List of plugin files.
     */
    public PluginFile[] collectResourceFiles(ExecutableConfig pluginConfig) {
        ObjectArrayList<PluginFile> result = new ObjectArrayList<PluginFile>();
        for (PluginFile file : pluginConfig.getFiles()) {
            result.add(file);

        }
        for (Resource resource : pluginConfig.getRequiredResources()) {
            collectResourceFiles(resource, result);
        }
        return result.toArray(new PluginFile[result.size()]);
    }


    private void collectResourceFiles(Resource resourceRef, ObjectArrayList<PluginFile> list) {
        ResourceConfig config = DependencyResolver.resolveResource(resourceRef.id, resourceRef.versionAtLeast, resourceRef.versionExactly,
                resourceRef.versionAtMost);
        if (config == null) {
            String message = String.format("Resource lookup failed for resourceRef id=%s versionAtLeast=%s versionExactly=%s versionAtMost=%s%n.",
                    config.getId(), resourceRef.versionAtLeast, resourceRef.versionExactly, resourceRef.versionAtMost);

            logger.error(message);
            throw new RuntimeException(message);
        } else {
            collectResourceFiles(list, config);
        }

    }

    public void collectResourceFiles(ObjectArrayList<PluginFile> list, ResourceConfig config) {
        for (Resource resourceRef2 : config.requires) {

            collectResourceFiles(resourceRef2, list);
        }
        for (PluginFile file : config.files) {

            // collect all but artifact install scripts (since they are fetched automatically)
            if (!"INSTALL".equals(file.id)) {
                logger.info("Collecting " + file.getLocalFile().getAbsolutePath());
                list.add(file);
            }

        }
    }

    /**
     * Write a constants.sh file, when running from the command line. When running from GobyWeb,
     * write the constants defined in 'replacements'.
     *
     * @param jobArea
     * @param job
     * @param taskLocalDir
     * @throws IOException
     */
    protected void writeConstants(JobArea jobArea, Job job, File taskLocalDir) throws IOException {
        //get the wrapper script
        URL constantsURL = getClass().getClassLoader().getResource(constantsTemplate);
        String constantsContent = IOUtils.toString(constantsURL);
        constantsContent = constantsContent
                .replaceAll("%%JOB_DIR%%", jobArea.getBasename(job.getTag()))
                .replaceAll("%%TAG%%", job.getTag())
                .replaceAll("%%ARTIFACT_REPOSITORY_DIR%%", artifactRepositoryPath);
        FileUtils.writeStringToFile(new File(taskLocalDir, constantsTemplate), constantsContent);

        if (environmentScriptFilename != null) {
            String data = IOUtils.toString(new FileReader(environmentScriptFilename));
            FileUtils.writeStringToFile(new File(taskLocalDir, constantsTemplate),
                    data, /* append */ true);

        }
    }
}


