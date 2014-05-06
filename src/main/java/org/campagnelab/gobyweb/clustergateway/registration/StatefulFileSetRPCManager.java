package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.filesets.rpc.FileSetClient;

import java.io.IOException;
import java.util.List;

/**
 * A stateful version of the FileSetManager that uses the RPC API.
 *
 * @author manuele
 */
public class StatefulFileSetRPCManager extends BaseStatefulManager {

    private final String clientName;

    private final String serverHost;

    private final int serverPort;

    FileSetClient client;

    public StatefulFileSetRPCManager(String filesetAreaReference, String owner, String clientName, int serverPort) throws IOException {
        super(filesetAreaReference, owner);
        this.clientName = clientName;
        this.serverPort = serverPort;
        this.serverHost = this.storageArea.getHostName();
        this.connect();
    }

    /**
     * Fetches the instance metadata.
     * @param tag
     * @param errors
     * @return
     * @throws IOException
     */
    public MetadataFileReader fetchMetadata(String tag, List<String> errors) throws IOException {
       return client.sendGetRequest(tag);
    }

    private void connect() throws IOException {
        this.client = new FileSetClient(this.clientName, this.storageArea.getRootPath(),
                this.storageArea.getOwner(), this.serverHost, this.serverPort);
    }
}
