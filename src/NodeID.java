import java.io.Serializable;

public class NodeID implements Serializable {
    private int identifier;

    public NodeID(int id) {
        identifier = id;
    }

    public int getID() {
        return identifier;
    }
}
