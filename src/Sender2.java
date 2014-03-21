/* Ignotas Sulzenko 1137931 */
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;


public class Sender2 {

	public final static int DATA_SIZE = 1027; // The size of the entire data part of the packet
	public final static int MESSAGE_SIZE = 1024; // The size of the meaningful part of the data part
	public final static int FEEDBACK_SIZE = 2; // The size of the feedback (Ack/Nak) message

	private DatagramSocket socket;
	private int portNumber, retryTimeout, retransmissions;
	private short sendPacketSeqNum;
	
	public Sender2(int portNum, int retryTimeout) throws Exception {
		this.portNumber = portNum;
		this.socket = new DatagramSocket();
		this.sendPacketSeqNum = 0;
		this.retransmissions = 0;
		this.retryTimeout = retryTimeout;
	}
		
	private void sendFile(String fileName) throws Exception {
		byte[] fileData = readData(fileName);
		long startTime = System.currentTimeMillis();
	    do {
	    	// Composing packets and sending them:
	    	sendNextPacket(constructPacket(fileData), this.sendPacketSeqNum);
			this.sendPacketSeqNum++;
		} while (this.sendPacketSeqNum * MESSAGE_SIZE < fileData.length);
	    long endTime = System.currentTimeMillis();
	    System.out.println("The transfer speed is: " + ((fileData.length / 1024) / ((endTime - startTime) / 1000.0)) + "kB/s");
		System.out.println("The total number of retransmissions is: " + this.retransmissions);
	    this.socket.close();
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
		int beginning = this.sendPacketSeqNum * MESSAGE_SIZE;
		int end = beginning + Math.min(MESSAGE_SIZE, data.length - beginning);
		byte[] packetData = new byte[end-beginning + DATA_SIZE - MESSAGE_SIZE];
		// Adding the main content:
		copyRange(packetData, 3, data, beginning, end);
		// Adding headers that encode the end of file and the packet sequence number:
		encodeSeqNumber(packetData, this.sendPacketSeqNum);
		encodeEndOfFile(packetData, (end == data.length));
		return new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), this.portNumber);
	}
	
	private void sendNextPacket(DatagramPacket packet, int sendSeqNum) throws Exception {
		// Sends the packet until it is received at the other end:
		byte[] buf;
		DatagramPacket receivedPacket;
		long startTime;
		int timeLeft;
		do {
			buf = new byte[FEEDBACK_SIZE];
			receivedPacket = new DatagramPacket(buf, buf.length);
			this.socket.send(packet);
			// We try to get an Ack, but if we don't get it within the retryTimeout we resend:
			try {
				startTime = System.currentTimeMillis();
				timeLeft = (int) (startTime + this.retryTimeout - System.currentTimeMillis());
				while (timeLeft > 0) {
					this.socket.setSoTimeout(timeLeft);
					this.socket.receive(receivedPacket);
					// If we get an ack for the packet that we've just sent, we return:
					if (decodeSeqNumber(receivedPacket.getData()) == sendSeqNum)
						return;
					timeLeft = (int) (startTime + this.retryTimeout - System.currentTimeMillis());
				}
			}
			catch (SocketTimeoutException e) {
				this.retransmissions++;
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
		int portNum = Integer.parseInt(args[1]);
		String fileName = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		Sender2 sender = new Sender2(portNum, retryTimeout);
		sender.sendFile(fileName);
	}
	
}