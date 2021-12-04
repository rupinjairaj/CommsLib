public class Main {
    public static void main(String[] args) {
        // Read NodeID and Config file from command line
        if (args.length < 2) {
            System.out.println(
                    "Insufficient args. Usage: \n\t java Main [nodeID] [config file name] [avg inter req delay] [avg cs exe time] [num of cs reqs]");
            return;
        }
        NodeID id = new NodeID(Integer.parseInt(args[0]));
        String configFile = args[1];
        int avgInterRequestDelay = Integer.parseInt(args[2]);
        int avgCSEcecustionTime = Integer.parseInt(args[3]);
        int numOfCSRequests = Integer.parseInt(args[4]);
        Application myApp = new Application(id, configFile, avgInterRequestDelay, avgCSEcecustionTime, numOfCSRequests);
        myApp.run();
    }
}
