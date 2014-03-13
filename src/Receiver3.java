/* Ignotas Sulzenko 1137931 */

public class Receiver3 {
	
	public static void main(String[] args) throws Exception {
		int portNum = Integer.parseInt(args[0]);
		String fileName = args[1];
		Receiver2 receiver = new Receiver2(portNum);
		receiver.receiveFile(fileName);
	}
	
}