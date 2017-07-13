package org.campagnelab.gobyweb.clustergateway.submission

import org.apache.commons.io.FileUtils
import org.campagnelab.gobyweb.clustergateway.registration.PluginsToConfigurations
import org.campagnelab.gobyweb.filesets.FileSetAPI
import org.campagnelab.gobyweb.filesets.configuration.ConfigurationList
import org.campagnelab.gobyweb.filesets.registration.cloud.Bucket
import org.campagnelab.gobyweb.filesets.registration.cloud.CloudEntry
import org.campagnelab.gobyweb.filesets.registration.core.BaseEntry
import org.campagnelab.gobyweb.io.AreaFactory
import org.campagnelab.gobyweb.io.FileSetArea
import org.campagnelab.gobyweb.plugins.PluginRegistry
import org.campagnelab.gobyweb.plugins.Plugins
import org.campagnelab.gobyweb.plugins.xml.Config
import org.campagnelab.gobyweb.plugins.xml.filesets.FileSetConfig
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Test integration with the google cloud platform.
 *
 * @author manuele
 */
@RunWith(JUnit4.class)
class ClusterGatewayCloudTest {

    private String clientId = "";
    private String clientSecret = "";
    private String bucketURL = "gs://wcmc_nw_training_data/";

    static final String gatewayPluginRoot = "test-data/gobyweb2-plugins";
    static final String envScript = "test-data/gobyweb2-plugins/artifacts-config/env.sh";
    static final String resultsDir = String.format("test-results/gateway-cloud");
    static final String owner = "junit";
    static final String repoDir = new File("${resultsDir}/REPO").getAbsolutePath()
    static final String filesetAreaRelativePath = "${resultsDir}/LOCAL_FILESETS_AREA"
    static final String jobArea = "${resultsDir}/LOCAL_JOB_AREA"
    static final String fileSetArea = new File(filesetAreaRelativePath).getAbsolutePath()
    FileSetAPI api;
    Plugins plugins;

    static Properties prop = new Properties();

    @BeforeClass
   static void configure() throws IOException {
        FileUtils.deleteDirectory(new File(resultsDir));
        FileUtils.forceMkdir(new File(resultsDir));
        try {
            FileUtils.deleteDirectory(new File(resultsDir));
            FileUtils.forceMkdir(new File(resultsDir));
            FileUtils.forceMkdir(new File(filesetAreaRelativePath));
        } catch (Exception e) {
            e.printStackTrace();
            fail("failed to create folders for storing the test results output");
        }
    }

    @Test
    void executePipeline() {
        loadPlugins();
        initFSA();
        List<String> txtTags = registerTXT();
        List<String> pngTags = registerPNG();
        submit(txtTags, pngTags)
    }

    void submit(List<String> texts, List<String> images) {
        def sdkArguments = ["--plugins-dir",
                            gatewayPluginRoot,
                            "--job",
                            "TARBALLER",
                            "--job-tag",
                            "AAABBBC",
                            "--owner",
                            owner,
                            "--queue",
                            "",
                            "--fileset-area",
                            fileSetArea,
                            "--job-area",
                            jobArea,
                            "--artifact-server",
                            "localhost",
                            "--repository",
                            repoDir,
                            "--cloud-id",
                            clientId,
                            "--cloud-secret",
                            clientSecret,
                            "--option",
                            "BASENAME=MYBASENAME"

        ];
        sdkArguments << "TEXT:"
        texts.each {
            sdkArguments << "${it.value}"
        }
        sdkArguments << "IMAGE:"
        images.each {
            sdkArguments << "${it.value}"
        }
        String[] combinedArgs = ((sdkArguments).flatten()) as String[];
        try {
            def code = ClusterGateway.processAPI(combinedArgs);
            assertTrue("Job submission failed. Submission returned the following exit code: ${code}", code != 0)
        } catch (Exception e) {
            fail("Job submission failed with exception.")

        }
    }

    def loadPlugins() {
        //load plugin configurations
        try {
            plugins = new Plugins();
            plugins.addServerConf(new File(gatewayPluginRoot).getAbsolutePath());
            plugins.setWebServerHostname("localhost");
            plugins.reload();
            assertTrue("Failed to load plugins definitions",!plugins.somePluginReportedErrors())

        } catch (Exception e) {
            fail("Failed to load plugins definitions");
        }

    }

    List<String> registerTXT() {
        List<String> tags;
        List<BaseEntry> inputEntries = new ArrayList<>()
        List<String> errors = new ArrayList<>()
        try {
            Bucket b = new Bucket(bucketURL);
            b.setContent(["testFive.txt"].toList())
            inputEntries.add(new CloudEntry(b,"TXT","1.0"));
            tags = api.registerURL(inputEntries,new HashMap<String, String>(), Collections.EMPTY_LIST, errors, null, null)
            assertTrue("Unexpected number of tags returned", tags.size() == 1)
        } catch (Exception e) {
            //logger.error("Request failed.");
            assertTrue("Failed to register filesets on the cloud bucket",true);
        }
        return tags;
    }

    List<String> registerPNG() {
        List<String> tags;
        List<BaseEntry> inputEntries = new ArrayList<>()
        List<String> errors = new ArrayList<>()
        try {
            Bucket b = new Bucket(bucketURL);
            b.setContent(["image1.png", "image2.png"].toList())
            inputEntries.add(new CloudEntry(b,"PNG","1.0"));
            tags = api.registerURL(inputEntries,new HashMap<String, String>(), Collections.EMPTY_LIST, errors, null, null)
            assertTrue("Unexpected number of tags returned", tags.size() == 2)
        } catch (Exception e) {
            //logger.error("Request failed.");
            assertTrue("Failed to register filesets on the cloud bucket",true);
        }
        return tags;
    }


    private void initFSA() {
        FileSetArea targetArea;
        try {
            targetArea = AreaFactory.createFileSetArea(fileSetArea,owner);
        } catch (Exception e) {
            throw new IOException("FileSetArea initialization failed", e)
        }
        api = FileSetAPI.getReadWriteAPI(targetArea, getFileSetConfigurationList())
    }

    private ConfigurationList getFileSetConfigurationList() {
        PluginRegistry registry = plugins.getRegistry()
        ConfigurationList allFS= PluginsToConfigurations.convertAsList(
                registry.filterConfigs(FileSetConfig.class).findAll({ Config conf -> !conf.disabled}))

        return allFS;
    }
}
