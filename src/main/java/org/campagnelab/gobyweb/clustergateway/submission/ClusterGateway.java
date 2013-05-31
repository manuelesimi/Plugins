package org.campagnelab.gobyweb.clustergateway.submission;


import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import org.apache.log4j.Logger;
import org.campagnelab.gobyweb.io.CommandLineHelper;

import java.io.IOException;


/**
 * Command line interface to the cluster gateway
 *
 * @author manuele
 */
public class ClusterGateway {

    protected static final org.apache.log4j.Logger logger = Logger.getLogger(ClusterGateway.class);

    public static void main(String[] args) {
        System.exit(process(args));
    }

    /**
     * Processes the caller request.
     * @param args
     * @return
     */
    public static int process(String[] args) {

        try {
            SubmissionRequest request = SubmissionRequestFactory.createRequest(args);
            return request.submitRequest();
        } catch (Exception e) {
            e.printStackTrace();
            //display the basic help
            try {
                JSAP jsap = new JSAP(ClusterGateway.class.getResource("ClusterGateway.jsap"));
                System.err.println(jsap.getHelp());
                System.err.println();
                System.err.println("Usage: java " + ClusterGateway.class.getName());
                System.err.println("                " + jsap.getUsage());
                System.err.println();
                return (1);
            } catch (Exception e1) {
                e1.printStackTrace();
                logger.error(e1);
                return (1);
            }
        }
    }

}
