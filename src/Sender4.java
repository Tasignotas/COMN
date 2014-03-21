/* Ignotas Sulzenko 1137931 */
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.*;
import java.util.HashMap;


public class Sender4 {

	public final static int DATA_SIZE = 1027; // The size of the entire data part of the packet
	public final static int MESSAGE_SIZE = 1024; // The size of the meaningful part of the data part
	public final static int FEEDBACK_SIZE = 2; // The size of the feedback (Ack) message

	private DatagramSocket socket;
	private int portNumber, windowSize, retryTimeout;
	private short base, nextSeqNum;
	// A hashmap that stores the timeout time for each non-ack'ed packet:
	private HashMap<Short, Long> packetTimeouts;
	
	public Sender4(int portNum, int retryTimeout, int windowSize) throws Exception {
		this.portNumber = portNum;
		this.windowSize = windowSize;
		this.socket = new DatagramSocket();
		this.retryTimeout = retryTimeout;
		this.packetTimeouts = new HashMap<Short, Long>();
		this.base = 0;
		this.nextSeqNum = 0;
	}
		
	private void sendFile(String fileName) throws Exception {
		byte[] fileData = readData(fileName);
		short end;
		long startTime = System.currentTimeMillis();
	    do {
	    	end = (short) Math.min(this.base + this.windowSize, 1 + Math.ceil(fileData.length/MESSAGE_SIZE));
	    	// Sending all of the packets that have to be sent and waiting for a response:
	    	sendPackets(this.nextSeqNum, end, fileData);
		} while (this.base * MESSAGE_SIZE < fileData.length);
	    long endTime = System.currentTimeMillis();
	    System.out.println("The transfer speed is: " + ((fileData.length / 1024) / ((endTime - startTime) / 1000.0)) + "kB/s");
		this.socket.close();
	}
	
	private void sendPackets(short beg, short end, byte[] fileData) throws Exception {
		DatagramPacket sendPacket;
		byte[] buf;
		long currentTime, earliestTimeout;
		int timeLeft;
		short decodedNum;
		DatagramPacket receivedPacket;
		buf = new byte[FEEDBACK_SIZE];
		receivedPacket = new DatagramPacket(buf, buf.length);
		// Sending all of the new packets:
		for (short x = beg; x < end; x++) {
			sendPacket = constructPacket(fileData, x);
			this.socket.send(sendPacket);
			// Recording the send time:
			this.packetTimeouts.put(x, System.currentTimeMillis() + this.retryTimeout);
			this.nextSeqNum++;
		}
		try {
			// We get the earliest timeout, because that's the maximum amount of time
			// we should be waiting for an Ack without resending:
			currentTime = System.currentTimeMillis();
			earliestTimeout = currentTime + 4;
			timeLeft = (int) (earliestTimeout - currentTime);
			while (timeLeft > 0) {
				this.socket.setSoTimeout(timeLeft);
				this.socket.receive(receivedPacket);
				decodedNum = decodeSeqNumber(receivedPacket.getData());
				// We remove the received packet from the non-Acked package hashmap:
				this.packetTimeouts.remove(decodedNum); 
				if (decodedNum == this.base) {
					// We move the base to the latest un-Ack'ed package:
					while (this.packetTimeouts.get(this.base) == null && this.base < this.nextSeqNum) {
						this.base++;
					}
					// We return from the method so that we could send some new packets:
					return;
				}
				currentTime = System.currentTimeMillis();
				timeLeft = (int) (earliestTimeout - currentTime);
			}
			resendTimeoutPackets(fileData);
		}
		catch (SocketTimeoutException e) {
			resendTimeoutPackets(fileData);
		}
	}
	
	private long getEarliestTimeout() {
		Long minTimeout = Long.MAX_VALUE;
		for(Short key : this.packetTimeouts.keySet()) {
			if (this.packetTimeouts.get(key) < minTimeout)
				minTimeout = this.packetTimeouts.get(key);
		}
		return minTimeout;
	}
	
	private void resendTimeoutPackets(byte[] fileData) throws Exception{
		// We resend each un-ack'ed packet if it timed out:
		DatagramPacket sendPacket;
		for(Short key : this.packetTimeouts.keySet()) {
			if (this.packetTimeouts.get(key) <= System.currentTimeMillis()) {
				sendPacket = constructPacket(fileData, key);
				this.socket.send(sendPacket);
				this.packetTimeouts.put(key, System.currentTimeMillis() + this.retryTimeout);
			}
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
		int beginning = packetNum * MESSAGE_SIZE;
		int end = beginning + Math.min(MESSAGE_SIZE, data.length - beginning);
		byte[] packetData = new byte[end - beginning + DATA_SIZE - MESSAGE_SIZE];
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
		int portNum = Integer.parseInt(args[1]);
		String fileName = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		int windowSize = Integer.parseInt(args[4]);
		Sender4 sender = new Sender4(portNum, retryTimeout, windowSize);
		sender.sendFile(fileName);
	}
	
}