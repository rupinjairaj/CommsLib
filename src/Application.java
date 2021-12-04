class Application {

    private NodeID myID;
    private String configFile;
    private int avgInterReqDelay;
    private int avgCSExeTime;
    private int numOfCSReqs;
    private DLock lock;
    public int activeNeighborsCount;

    public Application(NodeID id, String configFile, int avgInterReqDelay, int avgCSExeTime, int numOfCSReqs) {
        this.myID = id;
        this.configFile = configFile;
        this.avgInterReqDelay = avgInterReqDelay;
        this.avgCSExeTime = avgCSExeTime;
        this.numOfCSReqs = numOfCSReqs;
    }

    public synchronized void run() {
        this.lock = new DLock(this.myID, this.configFile);
        this.activeNeighborsCount = Config.getInstance().getNighbors().length;
        
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
                System.out.printf("Lock obtained. Node will now sleep for %d seconds to simulate 'work'\n",
                        avgCSExeTime);
                Thread.sleep(avgCSExeTime * 1000);
                System.out.println("Releasing lock.");
                this.lock.unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("System generated all its CS requests");
    }

}
