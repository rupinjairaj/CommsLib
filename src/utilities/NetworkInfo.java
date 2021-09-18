package utilities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NetworkInfo {

    private static NetworkInfo networkInfo = null;

    // total number of nodes in the system.
    private static int numberOfNodes = -1;

    // holds info about nodes and their neighbors.
    private static ArrayList<ArrayList<Integer>> neighbors = null;

    private static String hostName;

    private NetworkInfo() {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
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

    public void setNeighbor(int nodeID, ArrayList<Integer> neighborList) {
        if (numberOfNodes == -1)
            return;
        if (neighbors == null)
            neighbors = new ArrayList<>(numberOfNodes);
        neighbors.set(nodeID, neighborList);
    }

    public ArrayList<ArrayList<Integer>> getNeighbors() {
        return neighbors;
    }

}
