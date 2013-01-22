package org.campagnelab.gobyweb.plugins

import com.martiansoftware.jsap.JSAP
import com.martiansoftware.jsap.JSAPResult
import org.apache.commons.io.FilenameUtils
import org.apache.log4j.Logger
import org.campagnelab.gobyweb.artifacts.util.CommandExecutor
import org.campagnelab.gobyweb.plugins.xml.PluginConfig
import org.campagnelab.gobyweb.plugins.xml.Resource
import org.campagnelab.gobyweb.plugins.xml.ResourceConfig

/**
 * Tool to work with plugins from the command line.
 * @author Fabien Campagne
 * Date: 1/22/13
 * Time: 9:35 AM
 *
 */
class Main {

    private static Logger LOG = Logger.getLogger(Main.class);

    CommandExecutor commandExecutor
    Plugins plugins
    String pluginRoot
    /**
     * The remote directory where files are transferred. If the variable is not null, the directory
     * has been created remotely and must be removed on exit.
     */
    String remoteInstallDir
    String tmpDir = "\${TMPDIR}"
    String remoteRepoDir
    File environmentScriptLocalFilename;

    Main(String pluginRoot,
         String username,
         String remoteServer,
         String tmpDir,
         String remoteRepoDir) {

        this.pluginRoot = pluginRoot
        this.tmpDir = tmpDir
        this.commandExecutor = new CommandExecutor(username, remoteServer)
        this.commandExecutor.setQuiet(false)
        this.remoteRepoDir = remoteRepoDir
    }

    public static void main(String[] args) {
        JSAP jsap = new JSAP(Main.class.getResource("Main.jsap"));

        JSAPResult config = jsap.parse(args);

        if (!config.success() || config.getBoolean("help") || hasError(config)) {

            // print out the help, then specific error messages describing the problems
            // with the command line, THEN print usage.
            //  This is called "beating the user with a clue stick."

            System.err.println(jsap.getHelp());

            for (java.util.Iterator errs = config.getErrorMessageIterator();
            errs.hasNext();) {
                System.err.println("Error: " + errs.next());
            }

            System.err.println();
            System.err.println("Usage: java "
                    + Main.class.getName());
            System.err.println("                "
                    + jsap.getUsage());
            System.err.println();

            System.exit(1);
        }
        String pluginRoot = config.getString("plugin-root")
        String remoteServer = config.getString("deployment-server")
        String username = config.getString("username")
        String remoteRepo = config.getString("repository")
        String tmpDir = config.getString("tmp-dir")
        File environmentScriptLocalFilename = config.getFile("env-script")
        String action = "not-set"
        if (config.getBoolean("test-install")) {
            action = "test-install"
        }

        String[] pluginDescriptions = config.getStringArray("plugins")
        Main processor = new Main(pluginRoot, username, remoteServer, tmpDir, remoteRepo)
        processor.environmentScriptLocalFilename = environmentScriptLocalFilename
        processor.process(action, pluginDescriptions)

    }

    static Boolean hasError(JSAPResult jsapResult) {

        // must have at least one command:
        return !(jsapResult.getBoolean("test-install"))
    }

    def process(String action, String[] pluginDescriptions) {
        // test connection to remote server..
        if (commandExecutor.ssh("date") != 0) {
            System.err.println("Could not connect to remote host. Aborting.")
            System.exit 1
        }

        // load the plugins from plugin root
        plugins = new Plugins(pluginRoot)
        plugins.setWebServerHostname("localhost")
        plugins.reload()

        pluginDescriptions.each {
            it ->
                try {
                    switch (action) {

                        case "test-install":
                            testInstall(it)
                    }
                } finally {

                    cleanupInstallationDirectory()

                }
        }
    }


    def testInstall(String pluginDescription) {
        PluginRef ref = PluginRef.parseDescription(pluginDescription)
        if (ref == null) {
            throw new RuntimeException("Could not parse plugin reference: " + pluginDescription)
        }

        PluginConfig config = ref.instantiate(plugins)
        if (config == null) {
            throw new RuntimeException("Could not instantiate plugin: " + pluginDescription)
        }

        prepareInstallationDirectory()
        File pbRequests = plugins.createPbRequestFile(config);
        commandExecutor.scpToRemote(pbRequests.getAbsolutePath(), remotePath("install-requests.pb"))

        config.requires.each {
            resourceRef ->
                installResource(resourceRef)
        }
        // push environment script:
        commandExecutor.scpToRemote(environmentScriptLocalFilename.getCanonicalPath(), remotePath("env.sh"))
        commandExecutor.ssh("chmod +x " + remotePath("env.sh"));

        // run the installation for the plugin's artifacts:
        String runScriptContent = "#!/bin/bash\n" +
                ". ${remotePath("env.sh")}\n" +
                "java -Dtmp.io.dir=${tmpDir} -jar artifact-manager.jar --install " +
                "--repository ${this.remoteRepoDir} " +
                "--ssh-requests install-requests.pb"
        pushRunScript(runScriptContent, "run.sh")
        commandExecutor.ssh(remotePath("run.sh"));
        cleanupInstallationDirectory()
    }

    private void pushRunScript(String runScriptContent, String scriptFilename) {
        def tmpFile = File.createTempFile("run-", "script.sh")
        PrintWriter writer = new PrintWriter(tmpFile)
        writer.println(runScriptContent)
        writer.flush()
        commandExecutor.scpToRemote(tmpFile.getCanonicalPath(), remotePath(scriptFilename))
        commandExecutor.ssh("chmod +x " + remotePath(scriptFilename));
    }

    def installResource(Resource resource) {
        ResourceConfig config = plugins.lookupResource(resource.id, resource.versionAtLeast, resource.versionExactly)
        config.requires.each {
            resourceRef ->
                installResource(resourceRef)
        }
        config.files.each {
            file ->
                LOG.info("Transferring " + file.localFile.getName())
                int status = commandExecutor.scpToRemote(file.localFilename, remotePath(file.localFile.getName()))
                if (status != 0) {
                    throw new RuntimeException("Copying of plugin file failed for " + file.localFilename)
                }
        }
    }

    def remotePath(String remoteFilename) {
        return FilenameUtils.concat(remoteInstallDir, remoteFilename)
    }

    def prepareInstallationDirectory() {
        File tmpFile = File.createTempFile("install-", "-dir");
        String tmpName = tmpFile.getName();
        tmpFile.delete()
        int status
        def path = "${tmpDir}/${tmpName}"
        def command = "mkdir ${path}"
        status = commandExecutor.ssh(command);
        if (status != 0) {
            throw new RuntimeException("Unable to create remote work directory: " + path);
        }
        remoteInstallDir = path;
    }

    def cleanupInstallationDirectory() {
        if (this.remoteInstallDir != null) {

            System.err.println("Currently not removing remoteInstallDir, check  the path: " + remoteInstallDir)
            int status = 1;
            //       status=commandExecutor.ssh("rm -fr " + remoteInstallDir)
            if (status == 0) {
                this.remoteInstallDir = null;
            } else {
                //       throw new RuntimeException("Unable to remote remote work directory: " + remoteInstallDir);
            }
        }
    }
}
