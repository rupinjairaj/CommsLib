
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class NetworkInfo {

    private static NetworkInfo networkInfo = null;

    // total number of nodes in the system.
    private static int numberOfNodes = -1;

    // holds info about nodes and their neighbors.
    private static ArrayList<ArrayList<Integer>> neighbors = null;
    // dictionary 'network' holds info about nodes in the network. 
    // the key is the node ID.
    private static HashMap<Integer, NodeInfo> network;

    private static String hostName;

    private NetworkInfo() {
        network = new HashMap<>();
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // e.printStackTrace();
        }
    }

    public static NetworkInfo getInstance() {
        return networkInfo != null ? networkInfo : new NetworkInfo();
    }

    public String getHostName() {
        return NetworkInfo.hostName;
    }

    public void setNumberOfNodes(int nodes) {
        numberOfNodes = nodes;
    }

    public int getNumberOfNodes() {
        return numberOfNodes;
    }

    public void setNeighbor(ArrayList<Integer> neighborList) {
        if (numberOfNodes == -1)
            return;
        if (neighbors == null) {
            neighbors = new ArrayList<ArrayList<Integer>>(numberOfNodes);
        }
        neighbors.add(neighborList);
    }

    public void addNode(NodeInfo node) {
        if (network == null)
            return;
        network.put(node.id, node);
    }

    public NodeInfo getNode(int id) {
        if (network == null)
            return null;
        return network.get(id);
    }

    public ArrayList<ArrayList<Integer>> getNeighbors() {
        return neighbors;
    }

}
