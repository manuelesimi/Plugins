package org.campagnelab.gobyweb.clustergateway.registration;

import org.junit.Test;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: mas2182
 * Date: 3/28/13
 * Time: 1:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class InputEntryReaderTest {


    @Test
    public void testGetFiles() throws Exception {

        InputEntryScanner reader = new InputEntryScanner("**/*.java");
        for (File file : reader.getFiles()) {
            System.out.println(file.getAbsolutePath());
        }
    }
}
