import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;

class Sender1 {

	private final static int PACKET_SIZE = 1024;
	private static DatagramSocket socket;
	
	
	private static void sendFile(int portNum, String fileName) throws Exception {
		byte[] data;
		short seqNum = 0;
		int end;
		Path path = Paths.get(fileName);
		data = Files.readAllBytes(path);
		socket = new DatagramSocket();
		do {
			end = seqNum * PACKET_SIZE + Math.min(PACKET_SIZE, data.length - seqNum * PACKET_SIZE);
			RDTSend(Arrays.copyOfRange(data, seqNum * PACKET_SIZE, end), portNum, end == data.length, seqNum);
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