import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Message implements Serializable {
    NodeID source;
    byte[] payload;

    public Message(NodeID sourceId, byte[] payload) {
        this.source = sourceId;
        this.payload = payload;
    }

    public byte[] toBytes() {
        ObjectOutputStream oos = null;
        byte[] result = null;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            oos.flush();
            result = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Message getPayload(byte[] data) {
        ObjectInputStream ois = null;
        Message m = new Message(null, null);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
            ois = new ObjectInputStream(bis);
            m = (Message) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }
}
