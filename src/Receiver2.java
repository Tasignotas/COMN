/* Ignotas Sulzenko 1137931 */
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;


public class Receiver2 {
	
	private DatagramSocket receivingSocket, sendingSocket;
	private int receivingAtPortNum, sendingToPortNum, receivingPacketSeqNum;
	
	public Receiver2(int portNum) throws Exception {
		this.receivingAtPortNum = portNum;
		this.sendingToPortNum = 69;
		this.receivingSocket = new DatagramSocket(this.receivingAtPortNum);
		this.sendingSocket = new DatagramSocket();
	}
	
	private void receiveFile(String fileName) throws Exception {
		// Preparing ourselves for receiving packets:
		this.receivingPacketSeqNum = 0;
		byte[] packetData;
		boolean endOfFile = false;
		FileOutputStream out = new FileOutputStream(fileName);
		// We wait for a packet until the sender tells us that it's the end:
		while (!endOfFile) {
			packetData = receiveNextPacket();
			out.write(Arrays.copyOfRange(packetData, Sender2.DATA_SIZE - Sender2.MESSAGE_SIZE, packetData.length));
			this.receivingPacketSeqNum++;
			endOfFile = Sender2.decodeEndOfFile(packetData);
		}
		this.receivingSocket.close();
		out.close();
	}
		
	private byte[] receiveNextPacket() throws Exception {
		while (true) {
			byte[] buf = new byte[Sender2.DATA_SIZE];
			DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
			this.receivingSocket.receive(receivedPacket);
			sendAck(Sender2.decodeSeqNumber(receivedPacket.getData()));
			if (Sender2.decodeSeqNumber(receivedPacket.getData()) == this.receivingPacketSeqNum)
				return receivedPacket.getData();
		}
	}
	
	private void sendAck(short seqNum) throws Exception {
		byte[] packetData = new byte[Sender2.FEEDBACK_SIZE];
		Sender2.encodeSeqNumber(packetData, seqNum);
		this.sendingSocket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), this.sendingToPortNum));
	}
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		Receiver2 receiver = new Receiver2(portNum);
		receiver.receiveFile(fileName);
	}
	
}