import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Insufficient args.\nUsage: app [nodeID] [path to config file under this directory]");
            return;
        }
        // Node ID will be provided as an argument
        NodeID nodeID = new NodeID(Integer.parseInt(args[0]));
        // Getting config file
        String configFilePath = args[1]; // test: "/local-test/config"
        Node node = new Node(nodeID, configFilePath, new AppListener());
        byte[] payload = String.format("Hello from node %d!", nodeID.getID()).getBytes();
        Message m = new Message(nodeID, payload);
        int dest = nodeID.getID() == 0 ? 1 : 0;
        NodeID destNode = new NodeID(dest);
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        node.send(m, destNode);
        sc.close();
    }
}

class AppListener implements Listener {
    @Override
    public void receive(Message message) {
        String s = new String(message.payload, StandardCharsets.UTF_8);
        System.out.printf("Message from node %d: %s\n", message.source.getID(), s);
    }

    @Override
    public void broken(NodeID neighbor) {
        // TODO Auto-generated method stub
        // Ask prof what to do here
    }
}
