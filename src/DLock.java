import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DLock implements Listener {

    private NodeID nodeID;
    private Node node;
    private LogicalClock clock;
    private Payload pendingLockReq = null;

    // Stores all the lock request messages that
    // this node didn't reply to because its own
    // lock request has a smaller timestamp
    private Queue<NodeID> deferredRequests;

    private int pendingGrantMessages = 0;

    public DLock(NodeID identifier, String configFilename) {
        this.nodeID = identifier;
        this.clock = LogicalClock.getInstance(0);
        System.out.println("Establishing links with neighbors...");
        this.node = new Node(identifier, configFilename, this);
        System.out.println("Connection established with all neighbors!");
        this.pendingGrantMessages = Config.getInstance().getNighbors().length;
        this.deferredRequests = new ConcurrentLinkedDeque<>();
    }

    public synchronized void lock() {
        Payload reqLockPayload = new Payload(0, this.clock.getClockVal());
        Message reqLockMsg = new Message(this.nodeID, reqLockPayload.toBytes());
        this.clock.incrementClockVal();
        this.pendingLockReq = reqLockPayload;
        this.node.sendToAll(reqLockMsg);
        // wait until you get a grant msg from all nodes
        while (this.pendingGrantMessages > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void unlock() {
        this.pendingLockReq = null;
        this.pendingGrantMessages = Config.getInstance().getNighbors().length;
        while (!this.deferredRequests.isEmpty()) {
            NodeID deferredNodeID = this.deferredRequests.remove();
            this.sendGrantMessage(deferredNodeID);
        }
    }

    private synchronized void sendGrantMessage(NodeID destNodeID) {
        // passing along the clock for consistency although the clock is
        // not compared by the receiver for grant messages.
        Payload grantPayload = new Payload(1, this.clock.getClockVal());
        Message grantMsg = new Message(this.nodeID, grantPayload.toBytes());
        this.node.send(grantMsg, destNodeID);
    }

    @Override
    public synchronized void receive(Message message) {
        Payload inPayload = Payload.getPayload(message.data);
        System.out.printf("Received %d message from %d node (Incoming clock val: %d, Local clock Val: %d)\n",
                inPayload.messageType, message.source.getID(), inPayload.clockVal, this.clock.getClockVal());
        System.out.println("Updating local clock");
        this.clock.setClockVal(inPayload.clockVal);

        if (inPayload.messageType == 0) {
            // lock request message
            if (this.pendingLockReq == null || this.pendingLockReq.clockVal > inPayload.clockVal) {
                sendGrantMessage(message.source);
            } else if (this.pendingLockReq != null && this.pendingLockReq.clockVal == inPayload.clockVal
                    && this.nodeID.getID() < message.source.getID()) {
                this.deferredRequests.add(message.source);
            } else {
                sendGrantMessage(message.source);
            }
        } else if (inPayload.messageType == 1) {
            // lock grant message
            this.pendingGrantMessages--;
        } else {
            // this.activeNeighborsCount--;
        }
        notifyAll();
    }

    @Override
    public synchronized void broken(NodeID neighbor) {
        System.out.printf("Connection with node %d broken\n", neighbor.getID());
    }
}
