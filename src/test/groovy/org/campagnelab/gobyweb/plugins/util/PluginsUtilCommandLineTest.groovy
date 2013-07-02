package org.campagnelab.gobyweb.plugins.util

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tester for dependency tree visualization
 * @author manuele
 */
@RunWith(JUnit4.class)
class PluginsUtilCommandLineTest {

    @Test
    public void testDependencyTreeVisualization() {
       PluginsUtil.process(buildDependencyTreeArguments("GSNAP_GOBY"));

    }


    private static String[] buildDependencyTreeArguments(String pluginID) {
        ("--plugins-dir test-data/root-for-rnaselect " +
                "--action dependency-tree " +
                "--id ${pluginID} "
        ).split(" ");

    }
}
