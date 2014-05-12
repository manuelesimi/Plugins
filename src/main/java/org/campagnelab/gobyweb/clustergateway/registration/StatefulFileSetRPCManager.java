package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.filesets.rpc.FileSetClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A stateful version of the FileSetManager that uses the RPC API.
 *
 * @author manuele
 */
public class StatefulFileSetRPCManager extends BaseStatefulManager  {

    private final String clientName;

    private final String serverHost;

    private final int serverPort;

    private FileSetClient client;

    private static final Map<String,FileSetClient> fileSetClientMap = new HashMap<String, FileSetClient>();

    private static final long serialVersionUID = 1526246795622776347L;

    public StatefulFileSetRPCManager(String filesetAreaReference, String owner, String clientName, int serverPort) throws IOException {
        super(filesetAreaReference, owner);
        this.clientName = clientName;
        this.serverPort = serverPort;
        this.serverHost = this.storageArea.getHostName();
        String key = this.buildClientKey();
        if (fileSetClientMap.containsKey(key)) {
            //reuse the client
            this.client = fileSetClientMap.get(key);
            this.shutdown();
        }
        this.connect();
    }

    @Override
    public List<String> register(String fileSetID, String[] paths, Map<String, String> attributes,
                                 List<String> sharedWith, List<String> errors, String tag) throws Exception {
        return null;
    }

    /**
     * Fetches the instance metadata.
     * @param tag
     * @param errors
     * @return
     * @throws IOException
     */
    public MetadataFileReader fetchMetadata(String tag, List<String> errors) throws IOException {
       this.resetConnection();
       return client.sendGetRequest(tag);
    }

    /**
     * Checks if the connection with the server is still alive. If not, the connection is reopened.
     * @throws IOException
     */
    public void resetConnection() throws IOException {
        if (!client.isAlive()) {
            this.client.close();
            this.connect();
        }
    }

    /**
     * Checks if the connection with the server is still alive.
     */
    public boolean isAlive() {
        return this.client.isAlive();
    }

    /**
     * Shutdowns the connection.
     */
    public void shutdown() {
        this.client.close();
        fileSetClientMap.remove(this.buildClientKey());
    }

    public void connect() throws IOException {
        this.client = new FileSetClient(this.clientName, this.storageArea.getRootPath(),
                this.storageArea.getOwner(), this.serverHost, this.serverPort);
        fileSetClientMap.put(this.buildClientKey(),this.client);
    }

    private String buildClientKey() {
        return String.format("%s%d%s", this.serverHost,this.serverPort,this.clientName);
    }
}
