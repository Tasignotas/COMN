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
			// Getting the next packet:
			packetData = receiveNextPacket();
			// Writing the content out to the file:
			out.write(Arrays.copyOfRange(packetData.getData(), Sender2.DATA_SIZE - Sender2.MESSAGE_SIZE, packetData.getLength()));
			this.receivingPacketSeqNum++;
			endOfFile = Sender2.decodeEndOfFile(packetData.getData());
			// If it's the end of file, we close the output file,
			// but we keep the receiver opened so that the sender definitely gets the final ACK:
			if (endOfFile) {
				out.close();
				System.out.printf("Received the file %s\n", fileName);
			}	
		}
	}
		
	private DatagramPacket receiveNextPacket() throws Exception {
		while (true) {
			// We keep on trying to receive the next expected packet:
			byte[] buf = new byte[Sender2.DATA_SIZE];
			DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
			this.socket.receive(receivedPacket);
			sendAck(Sender2.decodeSeqNumber(receivedPacket.getData()), receivedPacket.getPort());
			// If we get what we want, we return the packet back:
			if (Sender2.decodeSeqNumber(receivedPacket.getData()) == this.receivingPacketSeqNum)
				return receivedPacket;
		}
	}
	
	private void sendAck(short seqNum, int senderPort) throws Exception {
		// We compose the acknowledgement and send it:
		byte[] packetData = new byte[Sender2.FEEDBACK_SIZE];
		// As specified, if it's not the packet that we were expecing
		// we send the acknowledgement to the most recent received packet:
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