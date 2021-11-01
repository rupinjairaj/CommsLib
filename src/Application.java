import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

/*Payload message type details:
	0 = requesting round info of roundNumber
	1 = sending round info for roundNumber
*/

class Application implements Listener {
    // This node's details.
    Node myNode;
    NodeID myID;

    String configFile;

    // List of IDs of neighbor for
    // this node.
    NodeID[] neighbors;

    // khop neighbors is a dictionary where
    // index 'k' represents the kth hop and
    // value is a list of integers representing
    // the IDs of the nodes reachable at 'k' hops
    // from this node.
    HashMap<Integer, ArrayList<Integer>> kHop;

    // The key represents the round for which the request
    // was made for. The value is a list of 'Message's
    // buffered for that round.
    HashMap<Integer, ArrayList<Message>> bufferedRequests;

    int pendingNeighborConfirmations = 0;

    // this represents the number of message this node
    // is still waiting for from its neighbors for the
    // current round that is being executed.
    int pendingMsgCount;

    int currRound;

    // A sorted set of nodes that are reachable by this
    // node for the current round
    TreeSet<Integer> currRoundHopNeighbors;
    TreeSet<Integer> nodesAlreadyReached;

    public Application(NodeID id, String configFile) {
        this.myID = id;
        this.configFile = configFile;
        this.kHop = new HashMap<>();
        this.currRoundHopNeighbors = new TreeSet<>();
        this.nodesAlreadyReached = new TreeSet<>();
        this.bufferedRequests = new HashMap<>();
    }

    public synchronized void decrementPendingMsgCnt() {
        this.pendingMsgCount--;
    }

    public synchronized void incrementPendingNeighborConfirmations() {
        this.pendingNeighborConfirmations++;
    }

    public synchronized void handleBufferedMessages() {
        if (!this.bufferedRequests.containsKey(this.currRound))
            return;

        ArrayList<Message> messages = this.bufferedRequests.get(this.currRound);
        for (Message reqMsg : messages) {
            Payload reqPayload = Payload.getPayload(reqMsg.data);
            Payload resPayload = new Payload(1, reqPayload.roundNumber, this.kHop.get(reqPayload.roundNumber));
            Message resMsg = new Message(this.myID, resPayload.toBytes());
            this.myNode.send(resMsg, reqMsg.source);
        }
        this.bufferedRequests.remove(this.currRound);
    }

    public void writeToFile() {
        // create file
        try {
            String[] sList = this.configFile.split("/");
            String s1 = sList[sList.length - 1];
            String fileName = String.format("%s-%s", this.myID.getID(), s1);
            File f = new File(fileName);
            if (f.createNewFile()) {
                System.out.println("Created the output file.");
            } else {
                System.out.println("Please delete the file that already exists with the same name and retry.");
                return;
            }
            FileWriter fw = new FileWriter(fileName, true);
            int roundNum = Config.getInstance().getNumberOfNodes();
            BufferedWriter bw = new BufferedWriter(fw);

            for (int i = 1; i < roundNum; i++) {
                StringBuilder builder = new StringBuilder();
                ArrayList<Integer> nodes = this.kHop.get(i);
                builder.append(i + ": ");
                // System.out.println("Round #: " + i);
                // System.out.print("Nodes: ");
                for (Integer node : nodes) {
                    builder.append(node + " ");
                    // System.out.print(node + " ");
                }
                bw.write(builder.toString());
                bw.newLine();
                // System.out.println();
            }
            bw.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void run() {
        // Setting up this node + establishing a connection
        // with all neighbors
        System.out.println("Setting up this node + establishing a connection with all neighbors.");
        this.myNode = new Node(myID, configFile, this);
        System.out.println("Connection with neigbhors established.");
        this.neighbors = myNode.getNeighbors();

        this.currRound = 1;
        // build the 1 hop neighbor
        for (NodeID node : neighbors) {
            this.currRoundHopNeighbors.add(node.getID());
        }

        Iterator<Integer> it = currRoundHopNeighbors.iterator();
        ArrayList<Integer> temp = new ArrayList<>();
        while (it.hasNext()) {
            int val = it.next();
            temp.add(val);
            this.nodesAlreadyReached.add(val);
        }
        this.nodesAlreadyReached.add(this.myID.getID());
        // save the 1 hop neighbor list
        this.kHop.put(currRound, temp);

        // loop through k-1 time to capture all the
        // 1..k-1 hop neighbors
        int k = Config.getInstance().getNumberOfNodes();
        for (int i = 1; i < k - 1; i++) {
            // we ask our neighbors for their
            // 'currRound' (k) hop neighbor info
            currRound = i;
            // handle any buffered requests before proceeding to
            // the next round.
            this.handleBufferedMessages();
            Payload reqPayload = new Payload(0, currRound, null);
            Message reqMessage = new Message(this.myID, reqPayload.toBytes());
            this.pendingMsgCount = neighbors.length;
            currRoundHopNeighbors.clear();
            System.out.println("Requesting neighbors for their " + currRound + " hop info.");
            myNode.sendToAll(reqMessage); // broadcast to all neighbors
            while (this.pendingMsgCount > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
            }
            it = currRoundHopNeighbors.iterator();
            temp = new ArrayList<>();
            while (it.hasNext()) {
                temp.add(it.next());
            }
            kHop.put(currRound + 1, temp);
        }
        Payload finPayload = new Payload(2, -1, null);
        Message finMsg = new Message(this.myID, finPayload.toBytes());
        this.myNode.sendToAll(finMsg);
        // wait for all neighbors to finish their task before shutting down.
        while (this.pendingNeighborConfirmations < this.neighbors.length) {
            try {
                wait();
            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
        }
        // handle any final buffered requests
        this.handleBufferedMessages();
        // print the output to the terminal
        // this has to be saved to a file (check project document)
        this.writeToFile();
    }

    @Override
    public synchronized void receive(Message message) {
        /**
         * NOTES: - You are requesting nodes for their 'currRound'(k-hop) info. You
         * already have your currRound info. So if a req comes in asking for your
         * currRound info don't buffer it. Buffer reqs only if they are for future
         * rounds (currRound + 1).
         */
        Payload resPayload = Payload.getPayload(message.data);

        if (resPayload.messageType == 0) {
            // handle request for round messages
            // from neighbors
            if (resPayload.roundNumber > this.currRound) {
                // buffer the message
                ArrayList<Message> temp;
                if (bufferedRequests.containsKey(resPayload.roundNumber)) {
                    temp = bufferedRequests.get(resPayload.roundNumber);
                } else {
                    temp = new ArrayList<>();
                }
                temp.add(message);
                bufferedRequests.put(resPayload.roundNumber, temp);
            } else {
                // respond to the message
                Payload res = new Payload(1, resPayload.roundNumber, kHop.get(resPayload.roundNumber));
                Message resMsg = new Message(this.myID, res.toBytes());
                this.myNode.send(resMsg, message.source);
            }
        } else if (resPayload.messageType == 1) {
            // handle response messages for
            // round info you requested
            System.out.println("Received response message from: " + message.source.getID() + " for Round#: "
                    + resPayload.roundNumber);
            for (Integer nodeID : resPayload.nodeIDs) {
                if (this.nodesAlreadyReached.contains(nodeID))
                    continue;
                this.nodesAlreadyReached.add(nodeID);
                currRoundHopNeighbors.add(nodeID);
            }
            decrementPendingMsgCnt();
        } else {
            // handle neighbor node completion message
            System.out.println("Node " + message.source.getID() + " completed its round collecting tasks.");
            this.incrementPendingNeighborConfirmations();
        }
        notifyAll();
    }

    @Override
    public void broken(NodeID neighbor) {
        System.out.println("Connection broken with node: " + neighbor.getID());
    }

}
