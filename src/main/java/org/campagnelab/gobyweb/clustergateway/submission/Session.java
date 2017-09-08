package org.campagnelab.gobyweb.clustergateway.submission;

/**
 * Session data for an {@link AbstractSubmitter} implementation.
 * @author manuele
 */
public class Session {

    public String callerAreaReferenceName;

    public String callerAreaOwner;

    public String targetAreaReferenceName;

    public String targetAreaOwner;

    public GoogleCloudConnection cloudConnection;

    protected Session () {}

    public boolean hasCloudAccess() {
        return cloudConnection != null;
    }

    public static class GoogleCloudConnection {

        public final String id;

        public final String secret;

        public final String token;

        public final String targetBucket;


        public GoogleCloudConnection(String cloudID, String cloudSecret, String token, String targetBucket) {
            this.id = cloudID;
            this.secret = cloudSecret;
            this.token = token;
            this.targetBucket = targetBucket;
        }
    }

}
