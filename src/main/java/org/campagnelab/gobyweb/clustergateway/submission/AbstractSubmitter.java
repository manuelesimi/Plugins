package org.campagnelab.gobyweb.clustergateway.submission;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.clustergateway.jobs.*;


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
import org.campagnelab.gobyweb.plugins.xml.Config;
import org.campagnelab.gobyweb.plugins.xml.common.PluginFile;
import org.campagnelab.gobyweb.plugins.xml.executables.*;
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig;
import org.campagnelab.gobyweb.plugins.xml.resources.Resource;
import org.campagnelab.gobyweb.plugins.xml.resources.ResourceConfig;


import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.*;

import static org.campagnelab.gobyweb.clustergateway.jobs.ExecutableJob.InvalidJobDataException;

/**
 * @author Fabien Campagne
 *         Date: 3/21/13
 *         Time: 10:27 AM
 */
abstract public class AbstractSubmitter implements Submitter {

    protected PluginRegistry registry;
    protected String environmentScriptFilename;
    protected String artifactRepositoryPath;
    protected String wrapperScripts[] = {"oge_job_script.sh.sh","oge_job_script_legacy.sh","common.sh"}; //default is OGE script for aligners and analyses
    protected String commonScript = "job_common_functions.sh"; //common functions
    protected String containerTechnology = "none";
    protected String containerName = "none";
    protected String queue;
    private static final File queueMessageDir = new File(System.getProperty("user.home") + "/.clustergateway/queue-message-dir");

    /**
     * The node from which the job will fetch artifacts. Can be local or remote to the submission machine
     */
    private String submissionHostname;

    /**
     * The folder with the plugins repos in the submission node
     */
    private File pluginsDir;

    private static Logger logger = Logger.getLogger(Submitter.class);

    protected String jobTag = null;

    protected String fileSetAreaReference;

    protected String submissionFileSetAreaReference;

    protected boolean useSubmissionFSA = false;

    protected String jobList="";


    protected AbstractSubmitter(PluginRegistry registry) throws IOException {
        this.registry = registry;
        if (!queueMessageDir.exists())
            FileUtils.forceMkdir(queueMessageDir);
    }

    @Override
    public Session newSession() {
        return new Session();
    }

    @Override
    public void setSubmissionHostname(String submissionHostname) {
        this.submissionHostname = submissionHostname;
    }


    @Override
    public void setLocalPluginsDir(File pluginsDir) {
        this.pluginsDir = pluginsDir;
    }

    @Override
    public void setWrapperScripts(String... wrapperScripts) {
        this.wrapperScripts = wrapperScripts;
    }

    /**
     * Sets the paths from where the script wrappers have to be loaded.
     *
     * @param paths
     */
    @Override
    public void setWrappersPaths(String[] paths) {
        Set<String> wrappers = new HashSet<String>();
        for (String path : paths) {
            PathPattern pathPattern = new PathPattern(path);
            for (File file : pathPattern.scan())
                wrappers.add(file.getAbsolutePath());
        }
        this.wrapperScripts = wrappers.toArray(new String[]{});
    }

    /**
     * Sets the type of script wrappers to use.
     *
     * @param technology
     */
    @Override
    public void setContainerTechnology(String technology, String ... name) {
     this.containerTechnology = technology;
     if (name != null && name.length >0)
        this.containerName = name[0];
    }

    @Override
    public void setEnvironmentScript(String environmentScriptFilename) {
        this.environmentScriptFilename = environmentScriptFilename;
    }


    /**
     * Sets the tag to assign to the job being submitted.
     *
     * @param jobTag
     */
    @Override
    public void assignTagToJob(String jobTag) {
        this.jobTag = jobTag;
    }

    /**
     * Comma-separated list of tags on which this job will depend on
     *
     * @param jobList
     */
    @Override
    public void setDependOnJobs(String jobList) {
        List<String> expandedJobList = new ArrayList<String>();
        for (String tag : jobList.split(",")) {
            expandedJobList.add(tag+".submit");
            expandedJobList.add(tag+".post");

        }
        this.jobList = Joiner.on(",").skipNulls().join(expandedJobList);
    }

    @Override
    public void setRemoteArtifactRepositoryPath(String artifactRepositoryPath) {
        assert artifactRepositoryPath != null : "artifactRepositoryPath cannot be null";
        this.artifactRepositoryPath = artifactRepositoryPath;
    }

    @Override
    public void setFileSetAreaReference(String fileSetAreaReference) {
        assert fileSetAreaReference != null : "fileSetAreaReference cannot be null";
        this.fileSetAreaReference = fileSetAreaReference;
    }

    @Override
    public void setSubmissionFileSetAreaReference(String fileSetAreaReference) {
        assert fileSetAreaReference != null : "fileSetAreaReference cannot be null";
        this.submissionFileSetAreaReference = fileSetAreaReference;
        this.useSubmissionFSA = true;
    }

    /**
     * Collects resource files for a resource and its dependencies.
     *
     * @param resourceConfig The resource config for which files are sought.
     * @return List of plugin files.
     */
    public PluginFile[] collectResourceFiles(ResourceConfig resourceConfig) {
        ObjectArrayList<PluginFile> result = new ObjectArrayList<PluginFile>();
        collectResourceFiles(result, resourceConfig);
        return result.toArray(new PluginFile[result.size()]);
    }


    protected void copyAutoOptions(ExecutableConfig executableConfig, File tempDir, JobRuntimeEnvironment environment) throws IOException {
        logger.info("Generating the job environment...");
        AutoOptionsFileHelper helper = new AutoOptionsFileHelper(registry);
        File autoOptionsFile = helper.generateAutoOptionsFile(executableConfig, null, null, environment);
        Files.copy(autoOptionsFile, new File(FilenameUtils.concat(tempDir.getAbsolutePath(), "auto-options.sh")));
    }


    protected void copyArtifactsPbRequests(Config config, String envScriptFilename, File tempDir) throws IOException {
        List<Config> configs = new ArrayList<>();
        configs.add(config);
        this.copyArtifactsPbRequests(configs,envScriptFilename,tempDir);

    }
    /**
     * Generates the artifacts PB request file for the executable job and copies to destination directory.
     *
     * @param configs
     * @param envScriptFilename
     * @param tempDir
     * @throws IOException
     */
    protected void copyArtifactsPbRequests(List<Config> configs, String envScriptFilename, File tempDir) throws IOException {
        if (this.pluginsDir == null) {
            throw new IOException("No plugins dir has been set for this submitter.");
        }
        ArtifactsProtoBufHelper helper = new ArtifactsProtoBufHelper();
        helper.setWebServerHostname(submissionHostname, this.pluginsDir.getAbsolutePath());
        if (envScriptFilename != null)
            helper.registerPluginEnvironmentCollectionScript(envScriptFilename);
        File installArtifactPbRequests;
        Config config = configs.get(0);
        if ((config.getClass().isAssignableFrom(ExecutableConfig.class)) //same class
                    || (ExecutableConfig.class.isInstance(config)))  //or a sub-class
                installArtifactPbRequests = helper.createPbRequestFile((ExecutableConfig) config);
        else {
            //as of version 2.5.5+, the SDK supports multiple plugins installation only for resources
            if (configs.size() == 1) {
                installArtifactPbRequests = helper.createPbRequestFile((ResourceConfig) config);
            } else {
                installArtifactPbRequests = helper.createPbRequestFileForMultipleResources((List<ResourceConfig>)(List<?>) configs);
            }
        }

        if (installArtifactPbRequests != null) {
            Files.copy(installArtifactPbRequests, new File(FilenameUtils.concat(tempDir.getAbsolutePath(), "artifacts-install-requests.pb")));
        }
    }

    protected void copyArtifactsPropertiesFiles(SubmissionRequest.ArtifactInfoMap attributes, File tempDir) throws IOException {
        if (attributes == null)
            return;
        for (String resource : attributes.getResources()) {
            for (String artifact : attributes.getArtifacts(resource)) {
                File dest = new File(tempDir.getAbsolutePath() + File.separator + "artifacts-values" + File.separator + resource + "." + artifact + ".properties");
                Files.createParentDirs(dest);
                Properties prop = new Properties();
                OutputStream output = null;
                try {
                    output = new FileOutputStream(dest.getAbsolutePath());
                    for (SubmissionRequest.AttributeValuePair attribute : attributes.getAttributes(resource,artifact))
                        prop.setProperty(attribute.name, attribute.value);
                    prop.store(output, null);
                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }
    }

    /**
     * Completes job environment with the information available in the submitter.
     *
     * @param job    the job
     * @param jobDir the target     execution directory
     */
    protected void completeJobEnvironment(ExecutableJob job, String jobDir) {
        JobRuntimeEnvironment environment = job.getEnvironment();
        environment.put("TAG", job.getTag());
        environment.put("OWNER", job.getOwnerId());
        environment.put("GOBYWEB_CONTAINER_TECHNOLOGY", this.containerTechnology);
        environment.put("GOBYWEB_CONTAINER_NAME", this.containerName);
        environment.put("JOB_PART_COMPLETED_STATUS", JobPartStatus.COMPLETED.statusName);
        environment.put("JOB_PART_FAILED_STATUS", JobPartStatus.FAILED.statusName);
        environment.put("JOB_PART_SPLIT_STATUS", JobPartStatus.SPLIT.statusName);
        environment.put("JOB_PART_ALIGN_STATUS", JobPartStatus.ALIGN.statusName);
        environment.put("JOB_PART_DIFF_EXP_STATUS", JobPartStatus.DIFF_EXP.statusName);
        environment.put("JOB_START_STATUS", JobPartStatus.START.statusName);
        environment.put("JOB_PART_SORT_STATUS", JobPartStatus.SORT.statusName);
        environment.put("JOB_PART_MERGE_STATUS", JobPartStatus.MERGE.statusName);
        environment.put("JOB_PART_CONCAT_STATUS", JobPartStatus.CONCAT.statusName);
        environment.put("JOB_PART_COUNTS_STATUS", JobPartStatus.COUNTS.statusName);
        environment.put("JOB_PART_WIGGLES_STATUS", JobPartStatus.WIGGLES.statusName);
        environment.put("JOB_PART_ALIGNMENT_STATS_STATUS", JobPartStatus.ALIGNMENT_STATS.statusName);
        environment.put("JOB_PART_ALIGNMENT_SEQ_VARIATION_STATS_STATUS", JobPartStatus.ALIGNMENT_SEQ_VARIATION_STATS.statusName);
        environment.put("JOB_PART_COMPRESS_STATUS", JobPartStatus.COMPRESS.statusName);
        environment.put("JOB_PART_TRANSFER_STATUS", JobPartStatus.TRANSFER.statusName);
        environment.put("JOB_REGISTERED_FILESETS_STATUS", JobPartStatus.REGISTERED_FILESETS.statusName);
        environment.put("JOB_KILLED_STATUS", JobPartStatus.KILLED.statusName);
        environment.put("JOB_DIR", jobDir);
        if (this.jobList.length() != 0)
            environment.put("JOBS_HOLD_LIST", this.jobList);
        environment.put("GOBY_DIR", "${TMPDIR}");
        environment.put("SGE_O_WORKDIR", jobDir);
        environment.put("KILL_FILE", String.format("%s/kill.sh", jobDir));
        environment.put("GRID_JVM_FLAGS", String.format("-Xms%dg -Xmx%dg", job.getMemoryInGigs(), job.getMemoryInGigs()));
        environment.put("QUEUE_NAME", this.queue);
        if (environment.containsKey("QUEUE_WRITER_POSTFIX")) {
            environment.put("QUEUE_WRITER", "${RESOURCES_GROOVY_EXECUTABLE} ${RESOURCES_GOBYWEB_SERVER_SIDE_QUEUE_WRITER} " + environment.getFromUndecorated("QUEUE_WRITER_POSTFIX"));
        } else {
            environment.put("QUEUE_WRITER", "${RESOURCES_GROOVY_EXECUTABLE} ${RESOURCES_GOBYWEB_SERVER_SIDE_QUEUE_WRITER} --handler-service PluginsSDK --queue-message-dir "
                    + queueMessageDir.getAbsolutePath());
        }
        environment.put("ARTIFACT_REPOSITORY_DIR", artifactRepositoryPath);
        environment.put("FILESET_AREA", String.format("%s/%s", fileSetAreaReference, job.getOwnerId()));
        environment.put("FILESET_TARGET_DIR", "${JOB_DIR}/source");
        environment.put("FILESET_COMMAND",
                String.format("java ${PLUGIN_NEED_DEFAULT_JVM_OPTIONS} -cp ${RESOURCES_GOBYWEB_SERVER_SIDE_FILESET_JAR}:${RESOURCES_MERCURY_LIB}:${RESOURCES_GOBYWEB_SERVER_SIDE_DEPENDENCIES_JAR} -Dlog4j.configuration=file:${RESOURCES_GOBYWEB_SERVER_SIDE_LOG4J_PROPERTIES} org.campagnelab.gobyweb.filesets.JobInterface --fileset-area-cache ${FILESET_TARGET_DIR} --pb-file %s/filesets.pb --aggregated-metadata-file %s/aggregated-metadata-file.pb --job-tag %s ",
                        jobDir,  jobDir,
                        job.getTag(),
                        "-a SOURCE_PLUGIN_ID=" + job.getSourceConfig().getId() + " -a SOURCE_PLUGIN_TYPE="+ job.getSourceConfig().getHumanReadableConfigType() )
        );
        if (job.useBroker()) {
            environment.put("BROKER_HOSTNAME", job.getBrokerHostname());
            environment.put("BROKER_PORT", job.getBrokerPort());
        }
        if (job.isParallel()) {
            environment.put("CPU_REQUIREMENTS", "#$ -l excl=true");
        } else {
            environment.put("CPU_REQUIREMENTS", "");
        }
        try {
            environment.put("WEB_SERVER_SSH_PREFIX", String.format("%s@%s", System.getProperty("user.name"),
                    java.net.InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException e) {
            logger.warn("unable to detect local host: WEB_SERVER_SSH_PREFIX won't be passed to the job, the job will not be able to send feedback.");
        }
    }

    /**
     * Copies resource files to a destination directory. Handles directories appropriately.
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
     * Copies resource files to a destination directory. Handles directories appropriately.
     *
     * @param resourceConfigs
     * @param tempDir
     * @throws IOException
     */
    protected void copyResourcesFiles(List<ResourceConfig> resourceConfigs, File tempDir) throws IOException {
        for (ResourceConfig resourceConfig : resourceConfigs) {
            this.copyResourceFiles(resourceConfig,tempDir);
        }
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
        logger.info("Collecting files from dependencies...");
        for (PluginFile file : collectResourceFiles(executableConfig)) {
            if (file.isDirectory) {
                FileUtils.copyDirectory(file.getLocalFile(), new File(tempDir, file.getLocalFile().getName()));
            } else {
                File to = new File(tempDir, file.getLocalFile().getName());
                Files.copy(file.getLocalFile(), to);
                if (file.getLocalFile().canExecute()) {
                    logger.trace(String.format("Marking %s with executable flag.", to.getAbsolutePath()));
                    to.setExecutable(true);
                }
                Files.copy(file.getLocalFile(), new File(tempDir, file.getLocalFile().getName()));
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
                    resourceRef.id, resourceRef.versionAtLeast, resourceRef.versionExactly, resourceRef.versionAtMost);

            logger.error(message);
            throw new RuntimeException(message);
        } else {
            collectResourceFiles(list, config);
        }

    }

    /**
     * Collects files from the resource and its dependencies, if any.
     *
     * @param list   the list where resource files are collected
     * @param config the resource configuration
     */
    public void collectResourceFiles(ObjectArrayList<PluginFile> list, ResourceConfig config) {
        logger.debug("Collecting resource files...");
        for (Resource resourceRef2 : config.requires)
            collectResourceFiles(resourceRef2, list);
        for (PluginFile file : config.files) {
            // collect all but artifact install scripts (since they are fetched automatically)
            if (!"INSTALL".equals(file.id)) {
                logger.debug("Collecting " + file.getLocalFile().getAbsolutePath());
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
    protected String writeConstants(JobArea jobArea, ResourceJob job) throws IOException {
        //get the wrapper script
        URL constantsURL = getClass().getClassLoader().getResource(constantsTemplate);
        String constantsContent = IOUtils.toString(constantsURL);
        return constantsContent
                .replaceAll("%%JOB_DIR%%", jobArea.getBasename(job.getTag()))
                .replaceAll("%%TAG%%", job.getTag())
                .replaceAll("%%ARTIFACT_REPOSITORY_DIR%%", artifactRepositoryPath)
                .replaceAll("%%PLUGIN_ID%%", job.getSourceConfigs().get(0).getId())
                .replaceAll("%%PLUGIN_VERSION%%", job.getSourceConfigs().get(0).getVersion()
                .replaceAll("%%GOBYWEB_CONTAINER_TECHNOLOGY%%", this.containerTechnology)
                .replaceAll("%%GOBYWEB_CONTAINER_NAME%%", this.containerName));
    }

    /**
     * Prepare the protocol buffer file to send to the cluster with the job
     *
     * @param session
     * @param job
     */
    protected File createJobDataPB(Session session, ExecutableJob job) throws InvalidJobDataException {

        //validate the IO data
        job.validateMandatorySlots();

        JobDataWriter jobDataWriter = new JobDataWriter();
         if (session.hasCloudAccess())  {
             //add data used by the job for returning information (typically, the IDs of the produced filesets
             jobDataWriter.addPushInfoWithCloud(new File(session.targetAreaReferenceName).getAbsolutePath(),
                     session.targetAreaOwner,
                     new File(session.callerAreaReferenceName).getAbsolutePath(),
                     session.callerAreaOwner, session.cloudConnection.id, session.cloudConnection.secret);
         } else {
             //add data used by the job for returning information (typically, the IDs of the produced filesets
             jobDataWriter.addPushInfo(new File(session.targetAreaReferenceName).getAbsolutePath(),
                     session.targetAreaOwner,
                     new File(session.callerAreaReferenceName).getAbsolutePath(),
                     session.callerAreaOwner);
         }
        ConfigurationList configurationList = new ConfigurationList();
        List<JobInputSlot> inputSlots = new ArrayList<JobInputSlot>();
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

            this.addConfigurationToList(configurationList, filesetConfig);

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
                throw new InvalidJobDataException(String.format("Unable to find a FileSet configuration matching the type of the output slot %s",
                        io.getName()));

            this.addConfigurationToList(configurationList, filesetConfig);
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
            new InvalidJobDataException("Failed to add the input slot list to protocol buffer", e);
        }

        jobDataWriter.addOutputSlotList(
                session.targetAreaReferenceName,
                session.targetAreaOwner,
                outputSlots);

        try {
            return jobDataWriter.serialize();
        } catch (Exception e) {
            throw new InvalidJobDataException("Failed to serialize the Job Data", e);
        }
    }

    private void addConfigurationToList(ConfigurationList configurationList, FileSetConfig filesetConfig) {
        Configuration configuration = new Configuration(filesetConfig.getId());
        configuration.setName(filesetConfig.getName());
        configuration.setVersion(filesetConfig.getVersion());
        for (FileSetConfig.ComponentSelector selector : filesetConfig.getFileSelectors()) {
            configuration.addFileSelector(
                    new Configuration.ComponentSelector(selector.getId(), selector.getPattern(), selector.getMandatory())
            );
        }
        for (FileSetConfig.ComponentSelector selector : filesetConfig.getDirSelectors()) {
            configuration.addDirSelector(
                    new Configuration.ComponentSelector(selector.getId(), selector.getPattern(), selector.getMandatory())
            );
        }
        configurationList.addConfiguration(configuration);
    }

    /**
     * Runs preDeployment scripts declared in the job plugin's configuration.
     *
     * @param job
     * @param jobDir
     */
    protected void runPreDeploymentScripts(ExecutableJob job, File jobDir) throws Exception {
        Execute execute = job.getSourceConfig().getExecute();
        if (execute != null) {
            for (Script script : execute.scripts()) {
                if (script.phase.equalsIgnoreCase("pre-deployment")) {
                    PluginScriptExecutor.executeScript(job, script, jobDir);
                }
            }
        }
    }

    /**
     * Prepares the wrapper script(s) for the job and copies them in the temporary dir.
     *
     * @param job     the job that will be executed by the submitter
     * @param tempDir the directory where to copy the wrapper script
     * @throws IOException
     */
    public void copyWrapperScripts(Job job, File tempDir) throws IOException {
        for (String wrapperScript : wrapperScripts) {

            copyOneWrapperScript(job, tempDir, wrapperScript);
        }
    }


    private void copyOneWrapperScript(Job job, File tempDir, String wrapperScript) throws
            IOException {
        //get the wrapper script wither from a file, or from the classpath:
        String wrapperContent = null;
        if (new File(wrapperScript).exists()) {
            wrapperContent = FileUtils.readFileToString(new File(wrapperScript));
        } else {
            URL wrapperScriptURL = getClass().getClassLoader().getResource(wrapperScript);
            assert wrapperScriptURL != null : "Unable to locate wrapperScript: " + wrapperScript;
            wrapperContent = IOUtils.toString(wrapperScriptURL);
        }
        wrapperContent = StringUtils.replace(wrapperContent, "\r", "");
        for (int i = 0; i < 2; i++) {
            // Do the replacements twice just in case replacements contain replacements
            for (Map.Entry<String, Object> replacement : job.getEnvironment().entrySet()) {
                String key = replacement.getKey();
                if (!key.startsWith("%") && !key.endsWith("%"))
                    key = "%"+ key + "%";
                wrapperContent = StringUtils.replace(wrapperContent, key,
                        (replacement.getValue() != null) ? replacement.getValue().toString() : "");
                if (replacement.getKey().startsWith("%"))
                    wrapperContent = StringUtils.replace(wrapperContent, replacement.getKey(),
                            (replacement.getValue() != null) ? replacement.getValue().toString() : "");
                else if (replacement.getKey().equalsIgnoreCase("PLUGIN_NEED_GLOBAL")) {
                    // PLUGIN_NEED_GLOBAL is not wrapped between % in the env,
                    // but it must be replaced in the script, so this is a special case
                    wrapperContent = StringUtils.replace(wrapperContent, "%" + replacement.getKey() + "%",
                            (replacement.getValue() != null) ? replacement.getValue().toString() : "");
                }
            }
        }
        FileUtils.writeStringToFile(new File(tempDir, wrapperScript), wrapperContent);
    }

    /**
     * Return the wrapper script to execute (main script).
     * @return
     */
    public String getWrapperScript() {
        return wrapperScripts[0];
    }
}


