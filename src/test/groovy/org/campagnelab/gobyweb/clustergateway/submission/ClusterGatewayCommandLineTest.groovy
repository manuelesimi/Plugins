package org.campagnelab.gobyweb.clustergateway.submission;

import org.apache.commons.io.FileUtils;
import org.campagnelab.gobyweb.clustergateway.runtime.JobArea;
import org.campagnelab.gobyweb.io.AreaFactory;
import org.campagnelab.gobyweb.io.FileSetArea;
import org.campagnelab.gobyweb.plugins.Plugins;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * This is written in groovy because it is easier to build command lines on the fly in this language.
 * @author Fabien Campagne
 *         Date: 3/22/13
 *         Time: 1:58 PM
 */
@RunWith(JUnit4.class)
public class ClusterGatewayCommandLineTest {

    static final String sourceStorageAreaDir = "test-data/cluster-gateway/command-line-tests";
    static final String pluginRoot = "test-data/root-for-gateway-command-line";
    static final String envScript = "test-data/env-scripts/env.sh";

    static final String resultsDir = String.format("test-results/gateway-command-line");
    static final String owner = "junit";
    static def repoDirAbsolutePath=new File("${resultsDir}/REPO").getAbsolutePath()


    @BeforeClass
    public static void configure() throws IOException {

        FileUtils.deleteDirectory(new File(resultsDir));
        FileUtils.forceMkdir(new File(resultsDir));

    }

    @Test
    public void installResourceWithArtifacts() {

       assertEquals(0, ClusterGateway.process(buildArgs("local","--resource MINIA:1.4961")));

    }


    private static String[] buildArgs(String remoteLocal, String additionalCommands) {
        ("--job-area ${resultsDir}/GOBYWEB_SGE_JOBS " +
                "--fileset-area ${resultsDir}/filesets " +
                "--plugins-dir ${pluginRoot} " +
                "--owner ${owner} " +
                "--env-script ${envScript} "+
                "--mode ${remoteLocal} " +
                "--artifact-server localhost "+
                "--repository ${repoDirAbsolutePath} "+
                additionalCommands).split(" ");

    }
}
