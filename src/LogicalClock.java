public class LogicalClock {
    private int clockVal;
    private static LogicalClock instance = null;

    private LogicalClock() {

    }

    private LogicalClock(int cVal) {
        this.clockVal = cVal;
    }

    public static LogicalClock getInstance(int initClockVal) {
        if (instance == null) {
            instance = new LogicalClock(initClockVal);
        }
        return instance;
    }

    public synchronized int getClockVal() {
        return this.clockVal;
    }

    public synchronized int incrementClockVal() {
        this.clockVal++;
        return this.clockVal;
    }

    public synchronized int setClockVal(int piggybackedVal) {
        this.clockVal = Math.max(this.clockVal, piggybackedVal);
        return this.clockVal;
    }
}
