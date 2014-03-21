/* Ignotas Sulzenko 1137931 */
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;


public class Receiver4 {
	
	private DatagramSocket socket;
	private int portNumber, windowSize;
	private HashMap<Short, DatagramPacket> packetDataBuf;
	private short base;
	
	public Receiver4(int portNum, int windowSize) throws Exception {
		this.portNumber = portNum;
		this.windowSize = windowSize;
		this.base = 0;
		this.packetDataBuf = new HashMap<Short, DatagramPacket>();
		this.socket = new DatagramSocket(this.portNumber);
	}
	
	public void receiveFile(String fileName) throws Exception {
		// Preparing ourselves for receiving packets:
		boolean endOfFile = false;
		FileOutputStream out = new FileOutputStream(fileName);
		// We wait for a packet until the sender tells us that it's the end:
		while (true) {
			receiveNewPackets();
			while (this.packetDataBuf.containsKey(this.base)) {
				endOfFile = Sender4.decodeEndOfFile(this.packetDataBuf.get(this.base).getData());
				out.write(Arrays.copyOfRange(this.packetDataBuf.get(this.base).getData(), Sender4.DATA_SIZE - Sender4.MESSAGE_SIZE, this.packetDataBuf.get(this.base).getLength()));
				this.base++;
			}
			if (endOfFile) {
				out.close();
			}
		}
	}
		
	private void receiveNewPackets() throws Exception {
		byte[] tempBuf;
		short seqNum;
		while (true) {
			tempBuf = new byte[Sender4.DATA_SIZE];
			DatagramPacket receivedPacket = new DatagramPacket(tempBuf, tempBuf.length);
			this.socket.receive(receivedPacket);
			seqNum = Sender4.decodeSeqNumber(receivedPacket.getData());
			if ((seqNum >= (this.base - this.windowSize)) && (seqNum < (this.base + this.windowSize))) {
				sendAck(seqNum, receivedPacket.getPort());
				this.packetDataBuf.put(seqNum, receivedPacket);
			}
			if (seqNum == this.base)
				break;
		}
	}
	
	private void sendAck(short seqNum, int senderPort) throws Exception {
		byte[] packetData = new byte[Sender2.FEEDBACK_SIZE];
		Sender2.encodeSeqNumber(packetData, seqNum);
		this.socket.send(new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), senderPort));
	}
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		int windowSize = Integer.parseInt(args[2]);
		Receiver4 receiver = new Receiver4(portNum, windowSize);
		receiver.receiveFile(fileName);
	}
	
}