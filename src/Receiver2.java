/* Ignotas Sulzenko 1137931 */
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;


public class Receiver2 {
	
	private DatagramSocket socket;
	private int portNumber;
	private short receivingPacketSeqNum;
	
	public Receiver2(int portNum) throws Exception {
		this.portNumber = portNum;
		this.socket = new DatagramSocket(this.portNumber);
	}
	
	public void receiveFile(String fileName) throws Exception {
		// Preparing ourselves for receiving packets:
		this.receivingPacketSeqNum = 0;
		DatagramPacket packetData;
		boolean endOfFile = false;
		FileOutputStream out = new FileOutputStream(fileName);
		// We wait for a packet until the sender tells us that it's the end:
		while (true) {
			packetData = receiveNextPacket();
			out.write(Arrays.copyOfRange(packetData.getData(), Sender2.DATA_SIZE - Sender2.MESSAGE_SIZE, packetData.getLength()));
			this.receivingPacketSeqNum++;
			endOfFile = Sender2.decodeEndOfFile(packetData.getData());
			if (endOfFile)
				out.close();
		}
	}
		
	private DatagramPacket receiveNextPacket() throws Exception {
		while (true) {
			byte[] buf = new byte[Sender2.DATA_SIZE];
			DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
			this.socket.receive(receivedPacket);
			sendAck(Sender2.decodeSeqNumber(receivedPacket.getData()), receivedPacket.getPort());
			if (Sender2.decodeSeqNumber(receivedPacket.getData()) == this.receivingPacketSeqNum)
				return receivedPacket;
		}
	}
	
	private void sendAck(short seqNum, int senderPort) throws Exception {
		byte[] packetData = new byte[Sender2.FEEDBACK_SIZE];
		if (seqNum != this.receivingPacketSeqNum)
			seqNum = (short) (this.receivingPacketSeqNum - 1);
		Sender2.encodeSeqNumber(packetData, seqNum);
		this.socket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), senderPort));
	}
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		Receiver2 receiver = new Receiver2(portNum);
		receiver.receiveFile(fileName);
	}
	
}