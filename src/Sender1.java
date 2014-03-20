/* Ignotas Sulzenko 1137931 */
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;


public class Sender1 {

	public final static int DATA_SIZE = 1027; // The size of the entire data part of the packet
	public final static int MESSAGE_SIZE = 1024; // The size of the meaningful part of the data part 
	
	private static void sendFile(int portNum, String fileName) throws Exception {
		// Initialising variables, preparing ourselves for sending the data:
		short seqNum = 0;
		byte[] packetData;
		DatagramSocket socket = new DatagramSocket();
		// Reading the data:
		File file = new File(fileName);
	    byte[] fileData = new byte[(int) file.length()];
	    DataInputStream dis = new DataInputStream(new FileInputStream(file));
	    dis.readFully(fileData);
	    dis.close();
	    do {
	    	// Composing packets and sending them:
	    	packetData = constructPacketData(seqNum, fileData);
			DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), portNum);
			socket.send(packet);
			Thread.sleep(10);
			seqNum++;
		} while (seqNum * MESSAGE_SIZE < fileData.length);
		socket.close();
	}
	
	private static void copyRange(byte[] target, int targetOffset, byte[] origin, int from, int to) {
		for (int x = from; x < to; x++) {
			target[targetOffset + (x - from)] = origin[x];
		}
	}
	
	private static byte[] constructPacketData(int seqNum, byte[] data) {
		int beginning = seqNum * MESSAGE_SIZE;
		int end = beginning + Math.min(MESSAGE_SIZE, data.length - beginning);
		System.out.println(end - beginning + DATA_SIZE - MESSAGE_SIZE);
		byte[] packetData = new byte[end-beginning + DATA_SIZE - MESSAGE_SIZE];
		// Adding headers that encode the end of file and the packet sequence number:
		packetData[0] = (end == data.length) ? (byte) 0x1 : (byte) 0x0;
		packetData[1] = (byte)(seqNum & 0xff);
		packetData[2] = (byte)((seqNum >> 8) & 0xff);
		// Adding the main content:
		copyRange(packetData, 3, data, beginning, end);
		return packetData;
	}
	
	public static void main(String[] args) throws Exception {
		sendFile(Integer.parseInt(args[1]), args[2]);
	}
	
}