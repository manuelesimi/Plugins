package org.campagnelab.gobyweb.clustergateway.submission;


import org.apache.log4j.Logger;


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
            logger.error(e);
            return (1);
        }
    }

}
