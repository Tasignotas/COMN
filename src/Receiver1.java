import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;



public class Receiver1 {
	
	
	private static void receiveFile(int portNum, String fileName) throws Exception {
		// Open the output file:
		FileOutputStream out = new FileOutputStream(fileName);
		// Open the socket to get the data coming to the specified port:
		DatagramSocket socket = new DatagramSocket(portNum);
		byte[] buf = new byte[1024];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		do {
			socket.receive(packet);
			out.write(packet.getData());
		} while (packet.getData()[0] != -1);
		socket.close();
		out.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		receiveFile(portNum, fileName);
	}
	
	
	
}