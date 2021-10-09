import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;

public class Node {
    private NodeID identifier;

    private int numberOfNodes;
    public HashMap<Integer, NodeInfo> network;
    private ArrayList<NodeID> neighbors;
    private Listener listener;

    public Node(NodeID identifier, String configFile, Listener listener) {
        this.identifier = identifier;
        this.network = new HashMap<Integer, NodeInfo>();
        this.neighbors = new ArrayList<NodeID>();
        this.readFile(configFile);
        this.listener = listener;
        NodeListener nodeListener = new NodeListener(listener, network.get(identifier.getID()).portNumber,
                this.neighbors, this.network);
        Thread listenerThread = new Thread(nodeListener, "th_serverListener");
        listenerThread.start();

        // start making connections
        connectWithNeighbours();
    }

    public NodeID[] getNeighbors() {
        return neighbors.toArray(new NodeID[0]);
    }

    public void send(Message message, NodeID destination) {
        NodeInfo dest = network.get(destination.getID());
        try {
            message.source = identifier;
            dest.outputStream.writeObject(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToAll(Message message) {
        for (NodeID nId : neighbors) {
            send(message, nId);
        }
    }

    public void tearDown() {
        for (NodeID nodeID : neighbors) {
            NodeInfo nodeInfo = network.get(nodeID.getID());
            if (!nodeInfo.socket.isConnected())
                continue;
            try {
                nodeInfo.outputStream.close();
                nodeInfo.inputStream.close();
                nodeInfo.socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(1);
    }

    /**
     * Helper methods of this class below this section
     */
    // Read and build network info from config file.

    // add a node to 'network'.
    public void addNode(NodeInfo node) {
        network.put(node.id, node);
    }

    // update the node's neighbour list
    public void addNeighbor(NodeID node) {
        NodeInfo neighbor = network.get(node.getID());
        // creates a socket to the neighbor node. It however
        // does not connect to it yet. Connection is handled the
        // first time a message is sent.
        neighbor.socket = new Socket();
        neighbors.add(node);
    }

    // If the line does not begin with a number
    // the the line is invalid.
    private boolean isLineValid(String line) {
        return !line.isEmpty() && Character.isDigit(line.charAt(0));
    }

    // remove comment if present in line
    private String cleanLine(String line) {
        if (line.indexOf('#') != -1)
            line = line.substring(0, line.indexOf('#'));
        return line.trim();
    }

    // get arraylist of space delimited values of each line.
    private String[] lineItems(String line) {
        return line.split(" ");
    }

    // helper method to get the next valid line from the file.
    private String getNextValidLine(BufferedReader reader) {
        String line = null;
        try {
            line = reader.readLine();
            while (line != null && !isLineValid(line)) {
                line = reader.readLine();
            }
            return line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    /**
     * Reads only valid lines from the configuration file. - First valid line is the
     * number of nodes in the system. - Next n valid lines are the details about the
     * node: eg: 0 dc02 12234 # node ID, host name, host port. - Next n valid lines
     * are mapping info about node and their neighbors. eg: 1 4 # space delimited
     * list of neighbors for node 0
     */
    public void readFile(String filePath) {
        File f = new File(getCurrentDir() + filePath);
        if (!f.exists() || f.isDirectory()) {
            System.out.println("No valid file path provided for Config file.");
            System.exit(1);
        }

        BufferedReader reader;
        String line;
        try {
            reader = new BufferedReader(new FileReader(getCurrentDir() + filePath));

            // first valid line
            line = getNextValidLine(reader);
            line = cleanLine(line);
            this.numberOfNodes = Integer.parseInt(line);

            // k valid lines hold info about nodes (i.e., node ID, host name & host port)
            int k = this.numberOfNodes;
            while (k > 0 && line != null) {
                k--;
                line = getNextValidLine(reader);
                line = cleanLine(line);
                String items[] = lineItems(line);
                int id = Integer.parseInt(items[0].trim());
                String hostName = items[1].trim();
                int port = Integer.parseInt(items[2].trim());
                NodeInfo node = new NodeInfo(id, hostName, port, null, null, null);
                this.addNode(node);
            }

            // n valid lines hold IDs of neighbors of kth node.
            k = 0;
            while (k < this.numberOfNodes && line != null) {
                k++;
                line = getNextValidLine(reader);
                if (k - 1 != this.identifier.getID())
                    continue;
                line = cleanLine(line);
                String items[] = lineItems(line);
                for (String i : items) {
                    this.addNeighbor(new NodeID(Integer.parseInt(i)));
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // helper method to get the dir of projects
    private String getCurrentDir() {
        return FileSystems.getDefault().getPath("").toAbsolutePath().toString();
    }

    // connect to peers with higher IDs
    private void connectWithNeighbours() {
        for (NodeID nodeID : neighbors) {
            if (nodeID.getID() < identifier.getID())
                continue;
            NodeInfo neighbourNode = network.get(nodeID.getID());
            // keep trying to establish a connection with
            // the peer every 5 seconds and exit the loop
            // once the connection is established.
            while (true) {
                // sleep for 5 seconds
                try {
                    // ignore resource leak warning. This socket will be closed during tearDown()
                    Socket socket = new Socket(neighbourNode.hostName, neighbourNode.portNumber);
                    if (socket.isConnected()) {
                        socket.setKeepAlive(true);
                        neighbourNode.socket = socket;
                        neighbourNode.outputStream = new ObjectOutputStream(socket.getOutputStream());
                        neighbourNode.inputStream = new ObjectInputStream(socket.getInputStream());
                        Message initMessage = new Message(this.identifier, null);
                        neighbourNode.outputStream.writeObject(initMessage);
                        network.put(neighbourNode.id, neighbourNode);
                        PeerListener peerListener = new PeerListener(this.listener, network.get(neighbourNode.id));
                        Thread listenerThread = new Thread(peerListener, "th_nodeListener" + neighbourNode.id);
                        listenerThread.start();
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Failed to connect with node: " + nodeID.getID() + " Retrying in 5 seconds...");
                    // e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class NodeListener implements Runnable {
    private Listener listener;
    private int port;
    private ArrayList<NodeID> neighbors;
    public HashMap<Integer, NodeInfo> network;

    public NodeListener(Listener listener, int port, ArrayList<NodeID> n, HashMap<Integer, NodeInfo> network) {
        this.listener = listener;
        this.port = port;
        this.neighbors = n;
        this.network = network;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                Socket s = null;

                try {
                    // a new connection
                    s = serverSocket.accept();
                    s.setKeepAlive(true);
                    ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
                    Message initMessage = (Message) inputStream.readObject();
                    for (NodeID id : neighbors) {
                        NodeInfo nodeInfo = network.get(id.getID());
                        if (nodeInfo.id == initMessage.source.getID()) {
                            nodeInfo.socket = s;
                            nodeInfo.outputStream = outputStream;
                            nodeInfo.inputStream = inputStream;
                            network.put(id.getID(), nodeInfo);
                            PeerListener peerListener = new PeerListener(listener, nodeInfo);
                            Thread listenerThread = new Thread(peerListener, "th_nodeListener" + nodeInfo.id);
                            listenerThread.start();
                        }
                    }
                } catch (Exception e) {
                    s.close();
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class PeerListener implements Runnable {
    private Listener listener;
    private NodeInfo nodeInfo;

    public PeerListener(Listener listener, NodeInfo nodeInfo) {
        this.listener = listener;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public void run() {
        while (true) {

            try {
                Message m = (Message) this.nodeInfo.inputStream.readObject();
                listener.receive(m);
            } catch (Exception e) {
                // e.printStackTrace();
                listener.broken(new NodeID(nodeInfo.id));
                break;
            }
        }
    }

}