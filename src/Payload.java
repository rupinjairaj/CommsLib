import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

//Payload needs to be serializable in order to convert it to byte array
class Payload implements java.io.Serializable {
	// Payload may be configured as per the need of the application
	// if type is 0 -> requesting round info of roundNumber
	// if type is 1 -> sending round info for roundNumber
	// if type is 2 -> indicating that the node is done collecting all k-1 round
	// info
	int messageType;
	int roundNumber;
	ArrayList<Integer> nodeIDs;

	// Constructor
	public Payload(int messageType, int roundNumber, ArrayList<Integer> nodeIDs) {
		this.messageType = messageType;
		this.nodeIDs = nodeIDs;
		this.roundNumber = roundNumber;
	}

	// Method to convert an instance of payload to a byte array
	public byte[] toBytes() {
		// Output streams help with serialization
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		byte[] result = null;
		try {
			oos = new ObjectOutputStream(bos);
			oos.writeObject(this);
			oos.flush();
			result = bos.toByteArray();
		} catch (Exception e) {
			System.out.println("Unable to serialize payload");
		} finally {
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}
		return result;
	}

	// Method to convert a byte array to an instance of payload
	public static Payload getPayload(byte[] data) {
		// Input streams help with deserialization
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream ois = null;
		ArrayList<Integer> list = new ArrayList<>();
		int roundNumber = 0;
		Payload p = new Payload(0, roundNumber, list);
		try {
			ois = new ObjectInputStream(bis);
			p = (Payload) ois.readObject();
		} catch (Exception e) {
			System.out.println("Unable to deserialize payload");
		} finally {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (IOException ioe) {
				// ignore close exception
			}
		}
		return p;
	}
}
