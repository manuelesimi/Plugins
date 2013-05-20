package org.campagnelab.gobyweb.plugins.util

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Created with IntelliJ IDEA.
 * User: manuelesimi
 * Date: 5/19/13
 * Time: 6:13 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(JUnit4.class)
class PluginsUtilCommandLineTest {

    @Test
    public void registerWithNameAndGuess() {
       PluginsUtil.process(buildDependencyTreeArguments("GSNAP_GOBY:1.1"));

    }


    private static String[] buildDependencyTreeArguments(String pluginID) {
        ("--plugins-dir test-data/root-for-rnaselect " +
                "--action dependency-tree " +
                "--id  ${pluginID} "
        ).split(" ");

    }
}
