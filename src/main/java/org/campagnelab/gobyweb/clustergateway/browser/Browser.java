package org.campagnelab.gobyweb.clustergateway.browser;

import com.martiansoftware.jsap.JSAPResult;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.io.CommandLineHelper;

import java.util.List;

/**
 * Browser interface.
 * It allows to browse the fileset area.
 *
 * @author manuele
 */
public class Browser {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(Browser.class);

    private static CommandLineHelper jsapHelper = new CommandLineHelper(Browser.class) {

    }  ;

        public static void main(String[] args) {
        try {
            process(args);
            System.exit(0);
        } catch (Exception e) {
            logger.error("FileSetManager failed to process the request.");
            System.exit(1);
        }
    }

    /**
     * Processes the caller requests.
     * @param args the arguments passed on the command line
     * @return the list of tags in case of register action, an empty list for the other operations
     * @throws Exception
     */
    public static void process(String[] args) throws Exception {
        JSAPResult config = jsapHelper.configure(args);
        if (config == null)
            System.exit(1);
    }
}
