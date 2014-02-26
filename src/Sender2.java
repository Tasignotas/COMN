/* Ignotas Sulzenko 1137931 */
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;


public class Sender2 {

	public final static int DATA_SIZE = 1024; // The size of the entire data part of the packet
	public final static int MESSAGE_SIZE = 1021; // The size of the meaningful part of the data part
	public final static int FEEDBACK_SIZE = 2; // The size of the feedback (Ack/Nak) message

	private DatagramSocket sendingSocket, receivingSocket;
	private int sendingToPortNum, receivingAtPortNum;
	private short sendPacketSeqNum;
	
	public Sender2(int sendPortNum, int retryTimeout) throws Exception {
		this.sendingToPortNum = sendPortNum;
		this.receivingAtPortNum = 69;
		this.sendingSocket = new DatagramSocket();
		this.receivingSocket = new DatagramSocket(this.receivingAtPortNum);
		this.receivingSocket.setSoTimeout(retryTimeout);
		this.sendPacketSeqNum = 0;
	}
		
	private void sendFile(String fileName) throws Exception {
		byte[] fileData = readData(fileName);
	    do {
	    	// Composing packets and sending them:
	    	sendNextPacket(constructPacket(fileData), this.sendPacketSeqNum);
			this.sendPacketSeqNum++;
		} while (this.sendPacketSeqNum * MESSAGE_SIZE < fileData.length);
		this.sendingSocket.close();
		this.receivingSocket.close();
	}
	
	private byte[] readData(String fileName) throws Exception {
		// Reading the data:
		File file = new File(fileName);
	    byte[] fileData = new byte[(int) file.length()];
	    DataInputStream dis = new DataInputStream(new FileInputStream(file));
	    dis.readFully(fileData);
	    dis.close();
	    return fileData;
	}
	
	public static void copyRange(byte[] target, int targetOffset, byte[] origin, int from, int to) {
		for (int x = from; x < to; x++) {
			target[targetOffset + (x - from)] = origin[x];
		}
	}
	
	private DatagramPacket constructPacket(byte[] data) throws Exception {
		byte[] packetData = new byte[DATA_SIZE];
		int beginning = this.sendPacketSeqNum * MESSAGE_SIZE;
		int end = beginning + Math.min(MESSAGE_SIZE, data.length - beginning);
		// Adding the main content:
		copyRange(packetData, 3, data, beginning, end);
		// Adding headers that encode the end of file and the packet sequence number:
		encodeSeqNumber(packetData, this.sendPacketSeqNum);
		encodeEndOfFile(packetData, (end == data.length));
		return new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), this.sendingToPortNum);
	}
	
	private void sendNextPacket(DatagramPacket packet, int sendSeqNum) throws Exception {
		// Sends the packet until it is received at the other end:
		byte[] buf;
		DatagramPacket receivedPacket;
		do {
			this.sendingSocket.send(packet);
			try {
				buf = new byte[FEEDBACK_SIZE];
				receivedPacket = new DatagramPacket(buf, buf.length);
				this.receivingSocket.receive(receivedPacket);
				if (decodeSeqNumber(receivedPacket.getData()) == sendSeqNum)
					break;
			}
			catch (SocketTimeoutException e) {
				continue;
			}
		} while (true);
	}
	
	public static void encodeSeqNumber(byte[] message, short seqNum) {
		// Encodes the sequence number to the first two bytes of the message:
		message[0] = (byte)(seqNum & 0xff);
		message[1] = (byte)((seqNum >> 8) & 0xff);
	}
	
	public static void encodeEndOfFile(byte[] message, boolean isEnd) {
		message[2] = isEnd ? (byte) 0x1 : (byte) 0x0;
	}
	
	public static short decodeSeqNumber(byte[] message) {
		return (short) ((message[1] << 8) + (message[0]&0xFF));
	}
	
	public static boolean decodeEndOfFile(byte[] message) {
		return (message[2] > 0) ? true : false;
	}
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		int retryTimeout = Integer.parseInt(args[2]);
		Sender2 sender = new Sender2(portNum, retryTimeout);
		sender.sendFile(fileName);
	}
	
}