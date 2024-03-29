import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int portNum;
    private int expectedNumOfCSReqs;
    private int numOfNodes;
    private int numOfActiveNodes;

    private int numOfCSReqsExecuted = 0;

    public Server() {
    }

    public synchronized int incNumOfCSReqsExecuted() {
        return numOfCSReqsExecuted++;
    }

    public synchronized int decNumOfActiveNode() {
        return this.numOfActiveNodes--;
    }

    public synchronized boolean targetComplete() {
        return numOfActiveNodes == 0;
    }

    public static void main(String[] args) {
        Server server = new Server();
        if (args.length < 2) {
            System.out.println(
                    "Insufficient args. Usage: \n\t java Server [port number] [expected num of cs reqs] [num of nodes]");
            return;
        }
        server.portNum = Integer.parseInt(args[0]);
        server.expectedNumOfCSReqs = Integer.parseInt(args[1]);
        server.numOfNodes = Integer.parseInt(args[2]);
        server.numOfActiveNodes = server.numOfNodes;
        ServerListener listener = new ServerListener(server, server.portNum);
        Thread listenerThread = new Thread(listener, "th_serverListener");
        listenerThread.start();
        try {
            listenerThread.join();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        assert server.expectedNumOfCSReqs == server.numOfCSReqsExecuted;
        // System.out.println("Number of CS requests: " + server.expectedNumOfCSReqs);
        // System.out.println("Number of CS Requests executed: " +
        // server.numOfCSReqsExecuted);
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
                        // lock acquired
                        server.incNumOfCSReqsExecuted();
                    } else if (payload.messageType == 4) {
                        // lock released
                    } else if (payload.messageType == 5) {
                        server.decNumOfActiveNode();
                    }
                    inputStream.close();
                    outputStream.close();
                } catch (Exception e) {
                    s.close();
                }
            }

        } catch (IOException e) {
            // e.printStackTrace();
        }
    }
}
