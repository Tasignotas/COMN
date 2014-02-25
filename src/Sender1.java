import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.util.Arrays;


class Sender1 {

	private final static int PACKET_SIZE = 1024;
	private static DatagramSocket socket;
	
	
	private static void sendFile(int portNum, String fileName) throws Exception {
		short seqNum = 0;
		int end;
		File file = new File("testfile.jpg");
	    byte[] data = new byte[(int) file.length()];
	    DataInputStream dis = new DataInputStream(new FileInputStream(file));
	    dis.readFully(data);
	    dis.close();
		socket = new DatagramSocket();
		do {
			end = seqNum * PACKET_SIZE + Math.min(PACKET_SIZE, data.length - seqNum * PACKET_SIZE);
			RDTSend(Arrays.copyOfRange(data, seqNum * PACKET_SIZE, end), portNum, end == data.length, seqNum);
			Thread.sleep(10);
			seqNum++;
		} while (seqNum * PACKET_SIZE < data.length);
	}
	
	
	private static void RDTSend(byte[] chunk, int portNum, boolean endOfFile, short seqNum) throws Exception {
		DatagramPacket packet = new DatagramPacket(chunk, chunk.length, InetAddress.getByName("localhost"), portNum);
		socket.send(packet);
		if (endOfFile)
			socket.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		sendFile(Integer.parseInt(args[0]), args[1]);
	}
	
}