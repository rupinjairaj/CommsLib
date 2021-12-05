import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class Application {

    private NodeID myID;
    private String configFile;
    private int avgInterReqDelay;
    private int avgCSExeTime;
    private int numOfCSReqs;
    private DLock lock;

    public Application(NodeID id, String configFile, int avgInterReqDelay, int avgCSExeTime, int numOfCSReqs) {
        this.myID = id;
        this.configFile = configFile;
        this.avgInterReqDelay = avgInterReqDelay;
        this.avgCSExeTime = avgCSExeTime;
        this.numOfCSReqs = numOfCSReqs;
    }

    private void notifyServer(int messageType) {
        try {
            Socket socket = new Socket("localhost", 8080);
            Payload payload = new Payload(messageType, -1);
            Message message = new Message(this.myID, payload.toBytes());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeObject(message);
            oos.close();
            ois.close();
            socket.close();
        } catch (Exception e) {
        }
    }

    public synchronized void run() {
        this.lock = new DLock(this.myID, this.configFile);

        // run this loop for 'numOfCSReqs' iterations
        while (numOfCSReqs-- > 0) {
            try {
                System.out.printf("Node going to sleep to simulate 'gap' b/w CS requests. Sleeping for %d seconds\n",
                        avgInterReqDelay);
                // sleep for avgInterReqDelay seconds
                Thread.sleep(avgInterReqDelay * 1000);
                System.out.printf("Node is awake. Generating CS req #: %d\n",
                        numOfCSReqs + 1);
                this.lock.lock();
                System.out.printf(
                        "Lock obtained. Node will first send the central server a message and then sleep for %d seconds to simulate 'work'\n",
                        avgCSExeTime);
                this.notifyServer(3);
                Thread.sleep(avgCSExeTime * 1000);
                System.out
                        .println(
                                "Node awake. Notifying the central server that it's leaving the CS and then releasing lock.");
                this.notifyServer(4);
                this.lock.unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
