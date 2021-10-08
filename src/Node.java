import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;

public class Node {
    private NodeID identifier;

    private int numberOfNodes;
    private HashMap<Integer, NodeInfo> network;
    private ArrayList<NodeID> neighbors;

    public Node(NodeID identifier, String configFile, Listener listener) {
        this.identifier = identifier;
        this.network = new HashMap<Integer, NodeInfo>();
        this.neighbors = new ArrayList<NodeID>();
        this.readFile(configFile);
        NodeListener nodeListener = new NodeListener(listener, network.get(identifier.getID()).portNumber);
        Thread listenerThread = new Thread(nodeListener, "th_serverListener");
        listenerThread.start();
    }

    public NodeID[] getNeighbors() {
        return neighbors.toArray(new NodeID[0]);
    }

    public void send(Message message, NodeID destination) {
        NodeInfo dest = network.get(destination.getID());
        try {
            if (!dest.socket.isConnected()) {
                InetAddress address = InetAddress.getByName(dest.hostName);
                SocketAddress socketAddress = new InetSocketAddress(address, dest.portNumber);
                dest.socket.connect(socketAddress);
                dest.outputStream = new ObjectOutputStream(dest.socket.getOutputStream());
                dest.inputStream = new ObjectInputStream(dest.socket.getInputStream());
            }
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
        // TODO: Implement this

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

        }

        BufferedReader reader;
        String line;
        try {
            reader = new BufferedReader(new FileReader(getCurrentDir() + filePath));

            // first valid line
            line = getNextValidLine(reader);
            System.out.println(line);
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
}

class NodeListener implements Runnable {
    private Listener listener;
    private int port;

    public NodeListener(Listener listener, int port) {
        this.listener = listener;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                Socket s = null;

                try {
                    s = serverSocket.accept();
                    s.setKeepAlive(true);
                    System.out.println("A new client has connected!");
                    ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream input = new ObjectInputStream(s.getInputStream());
                    Message m = (Message) input.readObject();
                    listener.receive(m);
                    input.close();
                    output.close();
                    s.close();
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
