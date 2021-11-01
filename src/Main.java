public class Main {
    public static void main(String[] args) {
        // Read NodeID and Config file from command line
        if (args.length < 2) {
            System.out.println(
                    "Insufficient args. Usage: \n\t java Main [nodeID] [path to config from pwd starting with '/']");
            return;
        }
        NodeID id = new NodeID(Integer.parseInt(args[0]));
        String configFile = args[1];

        // Launch application and wait for it to terminate
        // Application myApp = new Application(id, configFile);
        Application myApp = new Application(id, configFile);
        myApp.run();
        myApp.myNode.tearDown();
    }
}
