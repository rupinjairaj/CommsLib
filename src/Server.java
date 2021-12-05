import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

class LockDetails {
    public NodeID holder;
    public Date lockAcquiredAt;
    public Date lockReleaseAt;
}

public class Server {

    private int numOfCSReqs;
    private int numberOfNodes;

    // Should be equal to (numberOfNodes) * (numOfCSReqs)
    private int numOfCSReqsExecuted = 0;

    private List<LockDetails> history;

    HashMap<Integer, Integer> perNodeStatus;

    public Server() {
        this.perNodeStatus = new HashMap<>();
    }

    public synchronized int incNumOfCSReqsExecuted() {
        return numOfCSReqsExecuted++;
    }

    public synchronized boolean targetComplete() {
        return numOfCSReqsExecuted == (numOfCSReqs * numberOfNodes);
    }

    public synchronized void addLockAcquiredToHistory(NodeID nId) {
        LockDetails lockDetails = new LockDetails();
        lockDetails.holder = nId;
        lockDetails.lockAcquiredAt = Date.from(Instant.now());
        this.history.add(lockDetails);
    }

    public synchronized void addLockReleasedToHistory(NodeID nId) {
        if (this.history.get(this.history.size() - 1).holder.getID() != nId.getID()) {
            // because the last node to acquire the lock
            // should be the first to release it
            System.out.println("Impossible block!");
        }
        LockDetails lockDetails = this.history.get(this.history.size() - 1);
        lockDetails.lockReleaseAt = Date.from(Instant.now());
    }

    public static void main(String[] args) {
        Server server = new Server();
        if (args.length < 2) {
            System.out.println(
                    "Insufficient args. Usage: \n\t java Server [num of cs reqs] [num of nodes in the system]");
            return;
        }
        server.history = new ArrayList<>();
        server.numOfCSReqs = Integer.parseInt(args[0]);
        server.numberOfNodes = Integer.parseInt(args[1]);
        ServerListener listener = new ServerListener(server, 8080);
        Thread listenerThread = new Thread(listener, "th_serverListener");
        listenerThread.start();
        try {
            listenerThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class ServerListener implements Runnable {
    private int port;
    private Server server;

    public ServerListener(Server server, int port) {
        this.server = server;
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (!server.targetComplete()) {
                Socket s = null;

                try {
                    // a new connection
                    s = serverSocket.accept();
                    s.setKeepAlive(false);
                    ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
                    ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
                    Message msg = (Message) inputStream.readObject();
                    Payload payload = Payload.getPayload(msg.data);
                    if (payload.messageType == 3) {
                        server.incNumOfCSReqsExecuted();
                        int count = 0;
                        if (server.perNodeStatus.containsKey(msg.source.getID())) {
                            count = server.perNodeStatus.get(msg.source.getID());
                        }
                        server.perNodeStatus.put(msg.source.getID(), count + 1);
                        server.addLockAcquiredToHistory(msg.source);
                    } else if (payload.messageType == 4) {
                        server.addLockReleasedToHistory(msg.source);
                    }
                    outputStream.close();
                    inputStream.close();
                } catch (Exception e) {
                    s.close();
                }
            }

        } catch (IOException e) {
        }
    }
}
