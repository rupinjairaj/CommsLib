import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NodeInfo {
    public int id;
    public String hostName;
    public int portNumber;
    public Socket socket;
    public ObjectInputStream inputStream;
    public ObjectOutputStream outputStream;

    NodeInfo(int id, String hostName, int portNumber, Socket s, ObjectInputStream ois, ObjectOutputStream oos) {
        this.id = id;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.socket = s;
        this.inputStream = ois;
        this.outputStream = oos;
    }
}
