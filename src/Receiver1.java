/* Ignotas Sulzenko 1137931 */
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;


public class Receiver1 {
	
	private static void receiveFile(int portNum, String fileName) throws Exception {
		// Preparing ourselves for receiving packets:
		byte[] buf;
		short seqNum;
		boolean endOfFile = false;
		DatagramPacket packet;
		FileOutputStream out = new FileOutputStream(fileName);
		DatagramSocket socket = new DatagramSocket(portNum);
		// We wait for a packet until the sender tells us that it's the end:
		while (!endOfFile) {
			buf = new byte[Sender1.DATA_SIZE];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			// Decode the headers in the packet data:
			endOfFile = (packet.getData()[0] > 0) ? true : false;
			seqNum = (short) ((packet.getData()[2] << 8) + (packet.getData()[1]&0xFF));
			System.out.println(seqNum);
			// Save the main message part to the file:
			out.write(Arrays.copyOfRange(packet.getData(), Sender1.DATA_SIZE - Sender1.MESSAGE_SIZE, packet.getLength()));
		}
		socket.close();
		out.close();
	}
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		receiveFile(portNum, fileName);
	}
	
}