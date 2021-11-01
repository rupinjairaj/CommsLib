import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Node {
    private NodeID identifier;
    private Listener listener;
    private int connectedNeighborCount = 0;

    public Node(NodeID identifier, String configFile, Listener listener) {
        this.identifier = identifier;
        Config.getInstance().setIdentifier(identifier);
        Config.getInstance().readFile(configFile);
        this.listener = listener;
        NodeListener nodeListener = new NodeListener(listener, this);
        Thread listenerThread = new Thread(nodeListener, "th_serverListener");
        listenerThread.start();

        // start making connections
        connectWithNeighbours();
    }

    public NodeID[] getNeighbors() {
        return Config.getInstance().getNighbors();
    }

    public synchronized void incrementConnectedNeighborCount() {
        this.connectedNeighborCount++;
        notifyAll();
    }

    public synchronized int getConnectedNeighborCount() {
        return this.connectedNeighborCount;
    }

    public void send(Message message, NodeID destination) {
        NodeInfo dest = Config.getInstance().getNodeInfo(destination.getID());
        try {
            message.source = identifier;
            dest.outputStream.writeObject(message);
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    public void sendToAll(Message message) {
        NodeID[] nodeIDs = Config.getInstance().getNighbors();
        for (NodeID nId : nodeIDs) {
            send(message, nId);
        }
    }

    public void tearDown() {
        NodeID[] nodeIDs = Config.getInstance().getNighbors();
        for (NodeID nodeID : nodeIDs) {
            NodeInfo nodeInfo = Config.getInstance().getNodeInfo(nodeID.getID());
            if (!nodeInfo.socket.isConnected())
                continue;
            try {
                nodeInfo.outputStream.close();
                nodeInfo.inputStream.close();
                nodeInfo.socket.close();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
        System.exit(1);
    }

    // connect to peers with higher IDs
    private void connectWithNeighbours() {
        NodeID[] nodeIDs = Config.getInstance().getNighbors();

        for (NodeID nodeID : nodeIDs) {
            if (nodeID.getID() < identifier.getID())
                continue;
            NodeInfo neighbourNode = Config.getInstance().getNodeInfo(nodeID.getID());
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
                        Config.getInstance().setNodeInfo(neighbourNode);
                        PeerListener peerListener = new PeerListener(this.listener, neighbourNode);
                        Thread listenerThread = new Thread(peerListener, "th_nodeListener" + neighbourNode.id);
                        listenerThread.start();
                        this.incrementConnectedNeighborCount();
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Failed to connect with node: " + nodeID.getID() + " Retrying in 5 seconds...");
                    // e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        }
        // this block of code ensures that the Node class initialization call from
        // the Application does not relinquish control back to the Application until
        // a connection has been established with all its neighbors.
        while (this.getConnectedNeighborCount() != Config.getInstance().getNighbors().length) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }
}

class NodeListener implements Runnable {
    private Listener listener;
    private int port;
    private Node node;

    public NodeListener(Listener listener, Node node) {
        this.listener = listener;
        this.port = Config.getInstance().getNodeInfo(Config.getInstance().getIdentifier().getID()).portNumber;
        this.node = node;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (node.getConnectedNeighborCount() != Config.getInstance().getNighbors().length) {
                Socket s = null;

                try {
                    // a new connection
                    s = serverSocket.accept();
                    s.setKeepAlive(true);
                    ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
                    Message initMessage = (Message) inputStream.readObject();
                    NodeID[] neighbors = Config.getInstance().getNighbors();
                    for (NodeID id : neighbors) {
                        NodeInfo nodeInfo = Config.getInstance().getNodeInfo(id.getID());
                        // network.get(id.getID());
                        if (nodeInfo.id == initMessage.source.getID()) {
                            nodeInfo.socket = s;
                            nodeInfo.outputStream = outputStream;
                            nodeInfo.inputStream = inputStream;
                            Config.getInstance().setNodeInfo(nodeInfo);
                            // network.put(id.getID(), nodeInfo);
                            PeerListener peerListener = new PeerListener(listener, nodeInfo);
                            Thread listenerThread = new Thread(peerListener, "th_nodeListener" + nodeInfo.id);
                            listenerThread.start();
                            node.incrementConnectedNeighborCount();
                        }
                    }
                } catch (Exception e) {
                    s.close();
                    // e.printStackTrace();
                }
            }

        } catch (IOException e) {
            // e.printStackTrace();
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