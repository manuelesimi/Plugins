package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.io.Files;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob;
import org.campagnelab.gobyweb.clustergateway.jobs.Job;
import static org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob.*;

import org.campagnelab.gobyweb.filesets.protos.JobDataWriter;
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList;
import org.campagnelab.gobyweb.filesets.configuration.Configuration;
import org.campagnelab.gobyweb.filesets.jobschema.JobInputSlot;
import org.campagnelab.gobyweb.filesets.jobschema.JobOutputSlot;
import org.campagnelab.gobyweb.io.JobArea;
import org.campagnelab.gobyweb.plugins.ArtifactsProtoBufHelper;
import org.campagnelab.gobyweb.plugins.AutoOptionsFileHelper;
import org.campagnelab.gobyweb.plugins.DependencyResolver;
import org.campagnelab.gobyweb.plugins.PluginRegistry;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableConfig;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableInputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.ExecutableOutputSchema;
import org.campagnelab.gobyweb.plugins.xml.executables.Slot;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;

import java.util.ArrayList;
import java.util.List;
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
    protected static final String resourceInstallWrapperScript = "resource_install_wrapper_script.sh";


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
     * Prepare the variables needed when running from the command line. When running from GobyWeb,
     * write the constants defined in 'replacements'.
     *
     * @param jobArea
     * @param job
     * @return the content of the constant file
     * @throws IOException
     */
    protected String writeConstants(JobArea jobArea, Job job) throws IOException {
        //get the wrapper script
        URL constantsURL = getClass().getClassLoader().getResource(constantsTemplate);
        String constantsContent = IOUtils.toString(constantsURL);
        return constantsContent
                .replaceAll("%%JOB_DIR%%", jobArea.getBasename(job.getTag()))
                .replaceAll("%%TAG%%", job.getTag())
                .replaceAll("%%ARTIFACT_REPOSITORY_DIR%%", artifactRepositoryPath);
    }

    /**
     * Prepare the protocol buffer file to send to the cluster with the job
     * @param session
     * @param job
     */
    protected File createJobDataPB(Session session, ExecutableJob job) throws InvalidJobDataException {

        //validate the IO data
        job.validateMandatorySlots();

        JobDataWriter jobDataWriter = new JobDataWriter();

        //add data used by the job for returning information (typically, the IDs of the produced filesets
        jobDataWriter.addPushInfo(new File(session.targetAreaReferenceName).getAbsolutePath(),
                session.targetAreaOwner,
                new File(session.callerAreaReferenceName).getAbsolutePath(),
                session.callerAreaOwner);
        ConfigurationList configurationList = new ConfigurationList();
        List<JobInputSlot>  inputSlots = new ArrayList<JobInputSlot>();
        ExecutableInputSchema inputSchema = job.getInputSchema();
        //add the filesets and the sub-set of configurations visible to the job
        for (Slot io : inputSchema.getInputSlots()) {
            //look for the fileset configuration and add it to the configuration list
            FileSetConfig filesetConfig = DependencyResolver.resolveFileSet(io.geType().id,
                    io.geType().versionAtLeast,
                    io.geType().versionExactly,
                    io.geType().versionAtMost);
            if (filesetConfig == null)
               throw new InvalidJobDataException(String.format("Unable to find a FileSet configuration matching the type input slot %s", io.getName()));

            this.addConfigurationToList(configurationList,filesetConfig);

            //add the input slot
            JobInputSlot inputSlot = new JobInputSlot();
            inputSlot.name = io.getName();
            inputSlot.tags = job.getInputSlotValues(io.getName());
            inputSlot.id = io.geType().id;
            inputSlots.add(inputSlot);
        }

        List<JobOutputSlot> outputSlots = new ArrayList<JobOutputSlot>();
        ExecutableOutputSchema outputSchema = job.getOutputSchema();
        for (Slot io : outputSchema.getOutputSlots()) {
            //look for the fileset configuration and add it to the configuration list
            FileSetConfig filesetConfig = DependencyResolver.resolveFileSet(io.geType().id,
                    io.geType().versionAtLeast,
                    io.geType().versionExactly,
                    io.geType().versionAtMost);
            if (filesetConfig == null)
                throw new InvalidJobDataException(String.format("Unable to find a FileSet configuration matching the type of the input slot %s",
                        io.getName()));

            this.addConfigurationToList(configurationList,filesetConfig);
            //add the output slot
            JobOutputSlot outputSlot = new JobOutputSlot();
            outputSlot.name = io.getName();
            outputSlot.id = io.geType().id;
            outputSlots.add(outputSlot);
        }

        //add the configurations visible to the job
        jobDataWriter.addConfigurations(configurationList);

        //add input/output slots
        try {
            jobDataWriter.addInputSlotList(session.targetAreaReferenceName,
                    session.targetAreaOwner,
                    inputSlots);
        } catch (Exception e) {
            new InvalidJobDataException("Failed to add the input slot list to protocol buffer",e);
        }

        jobDataWriter.addOutputSlotList(
                session.targetAreaReferenceName,
                session.targetAreaOwner,
                outputSlots);

        try {
            return jobDataWriter.serialize();
        } catch (Exception e) {
            throw  new InvalidJobDataException("Failed to serialize the Job Data",e);
        }
    }

    private void addConfigurationToList(ConfigurationList configurationList, FileSetConfig filesetConfig) {
        Configuration configuration = new Configuration(filesetConfig.getId());
        configuration.setName(filesetConfig.getName());
        configuration.setVersion(filesetConfig.getVersion());
        for (FileSetConfig.ComponentSelector selector : filesetConfig.getFileSelectors()) {
            configuration.addFileSelector(
                    new Configuration.ComponentSelector(selector.getId(),selector.getPattern(),selector.getMandatory())
            );
        }
        for (FileSetConfig.ComponentSelector selector : filesetConfig.getDirSelectors()) {
            configuration.addDirSelector(
                    new Configuration.ComponentSelector(selector.getId(),selector.getPattern(),selector.getMandatory())
            );
        }
        configurationList.addConfiguration(configuration);
    }
}


