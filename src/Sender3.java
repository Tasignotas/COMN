/* Ignotas Sulzenko 1137931 */
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;


public class Sender3 {

	public final static int DATA_SIZE = 1024; // The size of the entire data part of the packet
	public final static int MESSAGE_SIZE = 1021; // The size of the meaningful part of the data part
	public final static int FEEDBACK_SIZE = 2; // The size of the feedback (Ack/Nak) message

	private DatagramSocket socket;
	private int portNumber, windowSize;
	private short base, nextSeqNum;
	
	public Sender3(int portNum, int retryTimeout, int windowSize) throws Exception {
		this.portNumber = portNum;
		this.windowSize = windowSize;
		this.socket = new DatagramSocket();
		this.socket.setSoTimeout(retryTimeout);
		this.base = 0;
		this.nextSeqNum = 0;
	}
		
	private void sendFile(String fileName) throws Exception {
		byte[] fileData = readData(fileName);
		short end;
		long startTime = System.currentTimeMillis();
	    do {
	    	end = (short) Math.min(this.base + this.windowSize, Math.ceil(fileData.length/MESSAGE_SIZE));
	    	// Sending all of the packets that have to be sent and waiting for a response:
	    	sendPackets(this.nextSeqNum, end, fileData);
		} while (this.base * MESSAGE_SIZE < fileData.length);
	    long endTime = System.currentTimeMillis();
	    System.out.println("The transfer speed is: " + ((fileData.length * 1024) / ((endTime - startTime) * 1000)) + "kB/s");
		this.socket.close();
	}
	
	private void sendPackets(short beg, short end, byte[] fileData) throws Exception {
		DatagramPacket sendPacket;
		byte[] buf;
		short decodedNum;
		DatagramPacket receivedPacket;
		buf = new byte[FEEDBACK_SIZE];
		receivedPacket = new DatagramPacket(buf, buf.length);
		// Sending the packets:
		for (short x = beg; x < end; x++) {
			sendPacket = constructPacket(fileData, x);
			this.socket.send(sendPacket);
			this.nextSeqNum++;
		}
		try {
			this.socket.receive(receivedPacket);
			decodedNum = decodeSeqNumber(receivedPacket.getData());
			if (decodedNum >= this.base)
				this.base = (short) (decodedNum + 1);
		}
		catch (SocketTimeoutException e) {
			this.nextSeqNum = this.base;
		}
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
	
	private DatagramPacket constructPacket(byte[] data, short packetNum) throws Exception {
		byte[] packetData = new byte[DATA_SIZE];
		int beginning = packetNum * MESSAGE_SIZE;
		int end = beginning + Math.min(MESSAGE_SIZE, data.length - beginning);
		// Adding the main content:
		copyRange(packetData, 3, data, beginning, end);
		// Adding headers that encode the end of file and the packet sequence number:
		encodeSeqNumber(packetData, packetNum);
		encodeEndOfFile(packetData, (end == data.length));
		return new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), this.portNumber);
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
		int windowSize = Integer.parseInt(args[3]);
		Sender3 sender = new Sender3(portNum, retryTimeout, windowSize);
		sender.sendFile(fileName);
	}
	
}