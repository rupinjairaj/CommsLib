import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Config {

    private static Config instance = null;

    private NodeID identifier = null;
    private int numberOfNodes;
    private HashMap<Integer, NodeInfo> network;
    private ArrayList<NodeID> neighbors;
    private int lowerIDNodeCnt = 0;

    private Config() {
        this.network = new HashMap<Integer, NodeInfo>();
        this.neighbors = new ArrayList<NodeID>();
    }

    public static Config getInstance() {
        if (instance == null)
            instance = new Config();

        return instance;
    }

    /**
     * Helper methods of this class below this section
     */
    public NodeID getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(NodeID id) {
        this.identifier = id;
    }

    public int getNumberOfNodes() {
        return this.numberOfNodes;
    }
    // Read and build network info from config file.

    // add a node to 'network'.
    public void addNode(NodeInfo node) {
        network.put(node.id, node);
    }

    public int getLowerIDNeighborCnt() {
        return this.lowerIDNodeCnt;
    }

    // update the node's neighbour list
    public void addNeighbor(NodeID node) {
        if (node.getID() < this.identifier.getID()) {
            this.lowerIDNodeCnt++;
        }
        NodeInfo neighbor = network.get(node.getID());
        // creates a socket to the neighbor node. It however
        // does not connect to it yet. Connection is handled the
        // first time a message is sent.
        neighbor.socket = new Socket();
        neighbors.add(node);
    }

    public NodeID[] getNighbors() {
        return neighbors.toArray(new NodeID[0]);
    }

    public NodeInfo getNodeInfo(int nodeID) {
        return network.get(nodeID);
    }

    public void setNodeInfo(NodeInfo nodeInfo) {
        network.put(nodeInfo.id, nodeInfo);
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
            // e.printStackTrace();
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
        File f = new File(filePath);
        if (!f.exists() || f.isDirectory()) {
            System.out.println("No valid file path provided for Config file.");
            System.exit(1);
        }

        BufferedReader reader;
        String line;
        try {
            reader = new BufferedReader(new FileReader(filePath));

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
            // e.printStackTrace();
        }
    }
}
