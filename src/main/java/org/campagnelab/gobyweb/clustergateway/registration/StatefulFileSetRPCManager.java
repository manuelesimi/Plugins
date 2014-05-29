package org.campagnelab.gobyweb.clustergateway.registration;

import org.campagnelab.gobyweb.filesets.configuration.Configuration;
import org.campagnelab.gobyweb.filesets.protos.MetadataFileReader;
import org.campagnelab.gobyweb.filesets.rpc.ClientNameAlreadyConnectedException;
import org.campagnelab.gobyweb.filesets.rpc.FetchedEntry;
import org.campagnelab.gobyweb.filesets.rpc.FileSetClient;
import org.campagnelab.gobyweb.plugins.PluginRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A stateful version of the FileSetManager that uses the RPC API.
 *
 * @author manuele
 */
public class StatefulFileSetRPCManager extends BaseStatefulManager {

    private final String clientName;

    private final String serverHost;

    private final int serverPort;

    /**
     * Username to use when sshing to the host that has the remote fileset area. Not the host with the tunnel,
     * but the host at the destination of the tunnel.
     */
    private final String filesetAreaUsername;
    private final String filesetAreaHostname;
    /**
     * The path to the fileset area on the remote fileset area.
     */
    private final String filesetAreaPath;
    private final String owner;

    private FileSetClient client;

    private static final Map<String, FileSetClient> fileSetClientMap = new HashMap<String, FileSetClient>();

    private static final long serialVersionUID = 1526246795622776347L;

    /**
     * Create a StatefulFileSetRPCManager.
     *
     * @param serverHost          The hostname of the server to which the fileset server connection will be made.
     * @param serverPort          The port of the server to which the fileset server connection will be made.
     * @param filesetAreaHostname The hostname of the server where the fileset area actually exists. This may be different from serverHost if a tunnel was created to the fileset server.
     * @param filesetAreaUsername The username to connect via ssh to the the server where the fileset area actually exists.
     * @param filesetAreaPath     The path to the fileset area on the fileset area server.
     * @param owner               The owner of the fileset instances that should be exposed, in the fileset area.
     * @param clientName          A client name that will be used to identify this connection.
     * @throws IOException
     */
    public StatefulFileSetRPCManager(String serverHost, int serverPort, String filesetAreaHostname,
                                     String filesetAreaUsername, String filesetAreaPath,
                                     String owner, String clientName) {
        super();
        this.pluginRegistry = PluginRegistry.getRegistry();
        this.owner = owner;
        this.clientName = clientName;
        this.serverPort = serverPort;
        this.serverHost = serverHost;
        this.filesetAreaHostname = filesetAreaHostname;
        this.filesetAreaUsername = filesetAreaUsername;
        this.filesetAreaPath = filesetAreaPath;
        String key = this.buildClientKey();
        if (fileSetClientMap.containsKey(key)) {
            //reuse the client
            this.client = fileSetClientMap.get(key);
            this.shutdown();
        }

    }

    @Override
    public List<String> register(String fileSetID, String[] paths, Map<String, String> attributes,
                                 List<String> sharedWith, List<String> errors, String tag) throws Exception {

        String[] entries = new String[paths.length + 1];
        entries[0] = fileSetID + ":";
        System.arraycopy(paths, 0, entries, 1, paths.length);
        Configuration filesetConfiguration = this.configurationList.getConfiguration(fileSetID);
        if (filesetConfiguration == null) {
            errors.add("Unable to find a FileSet configuration with id=" + fileSetID);
            return Collections.emptyList();
        }
        return this.client.sendRegisterRequest(entries, filesetConfiguration, attributes, sharedWith, errors, tag);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }

    /**
     * Fetches the instance metadata.
     *
     * @param tag
     * @param errors
     * @return
     * @throws IOException
     */
    public MetadataFileReader fetchMetadata(String tag, List<String> errors) throws IOException {
//        this.resetConnection();
        return client.sendGetRequest(tag);
    }

    /**
     * Fetches an entry from the instance.
     *
     * @param entryName
     * @param tag
     * @param paths
     * @param errors
     * @return
     * @throws java.io.IOException
     */
    @Override
    public boolean fetchEntry(String entryName, String tag, List<String> paths, List<String> errors) throws IOException {
         throw new UnsupportedOperationException();
    }

    /**
     * Fetches an entry from the instance.
     *
     * @param entryName
     * @param tag
     * @param data
     * @param errors
     * @return
     * @throws java.io.IOException
     */
    @Override
    public boolean fetchStreamedEntry(String entryName, String tag, List<ByteBuffer> data, List<String> errors) throws IOException {
        FetchedEntry entry = client.fetchEntry(entryName,tag,errors);
        if (entry == null || entry.size() == 0)
            return false;
        if (data == null)
            data = new ArrayList<ByteBuffer>();
        for (int i = 0; i < entry.size(); i++)
            data.add(entry.getDataAt(i));
        return true;
    }

    /**
     * Checks if the connection with the server is still alive. If not, the connection is reopened.
     *
     * @throws IOException
     */
    public void resetConnection() throws IOException, ClientNameAlreadyConnectedException {
        if (!isAlive()) {
            this.client.close();
            this.connect();
        }
    }

    /**
     * Checks if the connection with the server is still alive.
     */
    public boolean isAlive() {
        return client != null && this.client.isAlive();
    }

    /**
     * Shutdowns the connection.
     */
    public void shutdown() {
        if (isAlive()) {
            this.client.close();
        }
        fileSetClientMap.remove(this.buildClientKey());

    }

    public void connect() throws IOException, ClientNameAlreadyConnectedException {
        this.client = new FileSetClient(
                this.clientName,
                filesetAreaHostname,
                filesetAreaUsername,
                filesetAreaPath,
                this.owner,
                this.serverHost,
                this.serverPort);
        fileSetClientMap.put(this.buildClientKey(), this.client);
    }

    private String buildClientKey() {
        return String.format("%s%d%s", this.serverHost, this.serverPort, this.clientName);
    }
}
