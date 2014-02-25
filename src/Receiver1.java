import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;



public class Receiver1 {
	
	
	private static void receiveFile(int portNum, String fileName) throws Exception {
		byte[] buf;
		DatagramPacket packet;
		// Create the output file:
		FileOutputStream out = new FileOutputStream(fileName);
		// Open the socket to get the data coming to the specified port:
		DatagramSocket socket = new DatagramSocket(portNum);
		int total = 0;
		do {
			buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			out.write(packet.getData());
			System.out.println(total);
		} while (total++ < 2049);
		socket.close();
		out.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		receiveFile(portNum, fileName);
	}
	
	
	
}