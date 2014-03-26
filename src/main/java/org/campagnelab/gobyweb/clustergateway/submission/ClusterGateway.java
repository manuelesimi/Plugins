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
     * Processes the caller request from the command line.
     * @param args
     * @return one of the following codes:
     *  1: an error occurred when the arguments have been parsed
     *  2: failed to access the Job Area
     *  3: submission to cluster failed
     *  4: unused
     *  5: invalid JSAP request
     */
    public static int process(String[] args) {

        try {
            SubmissionRequest request = SubmissionRequestFactory.createRequest(args);
            return request.submitRequest(false);
        } catch (Exception e) {
            //display the basic help
            try {
                JSAP jsap = new JSAP(ClusterGateway.class.getResource("ClusterGateway.jsap"));
                System.err.println(jsap.getHelp());
                System.err.println();
                System.err.println("Usage: java " + ClusterGateway.class.getName());
                System.err.println("                " + jsap.getUsage());
                System.err.println();
                System.err.println(e.getMessage());
                return (5);
            } catch (Exception e1) {
                e1.printStackTrace();
                logger.error(e1);
                return (6);
            }
        }
    }

    /**
     * Version of process method to be invoked as API
     * @param args
     * @return
     * @throws Exception
     */
    public static int processAPI(String[] args) throws Exception {
        SubmissionRequest request = SubmissionRequestFactory.createRequest(args);
        return request.submitRequest(true);
    }
}
