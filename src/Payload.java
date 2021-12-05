import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

//Payload needs to be serializable in order to convert it to byte array
class Payload implements java.io.Serializable {
	// Payload may be configured as per the need of the application
	// if type is 0 -> requesting lock access
	// if type is 1 -> granting lock access
	// if type is 2 -> the peer has finished and will not send messages anymore
	// if type is 3 -> the peer sends this to the central server to denote it holds
	// the lock and is excuting the CS
	// if type is 4 -> the peer sends this to the central server to denote it holds
	// the lock and is excuting the CS
	// if type is 5 -> the peer sends this to the central server to denote it is
	// done executing all its CS tasks
	int messageType;
	int clockVal;

	// Constructor
	public Payload(int messageType, int clockVal) {
		this.messageType = messageType;
		this.clockVal = clockVal;
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
		Payload p = new Payload(0, 0);
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
