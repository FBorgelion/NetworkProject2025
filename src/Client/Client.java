package Client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;

import Shared.MyPacket;

public class Client {
	
	private static int performHandshake(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
        for (int i = 0; i < 3; i++) {
            sendSynPacket(socket, address, port, seqNum);
            System.out.println("Try " + (i + 1));
            if (waitForSynAck(socket)) {
            	System.out.println("SYN+ACK received");
                seqNum++;
            	sendSynPacket(socket, address, port, seqNum);
                System.out.println("Handshake completed.");
                return seqNum+1;
            }
			System.err.println("No response. Try " + (i + 1));
        }
        return -1;
    }
	
	public static void sendSynPacket(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {		
		MyPacket mySynPacket = new MyPacket(seqNum, true, false, false, false, new byte[0]);
		byte[] synBytes = mySynPacket.toBytes();
		
		DatagramPacket synPacket = new DatagramPacket(synBytes, synBytes.length, address, port);
		socket.send(synPacket);
		
		System.out.println("SYN sent. Sequence number " + seqNum);
	}
	
	public static void sendFinPacket(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {		
		MyPacket myFinPacket = new MyPacket(seqNum, false, false, true, false, new byte[0]);
		byte[] finBytes = myFinPacket.toBytes();
		
		DatagramPacket finPacket = new DatagramPacket(finBytes, finBytes.length, address, port);
		socket.send(finPacket);
		
		System.out.println("FIN sent. Sequence number " + seqNum);
	}
	
	public static void sendAckPacket(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {		
		MyPacket myAckPacket = new MyPacket(seqNum, false, true, false, false, new byte[0]);
		byte[] ackBytes = myAckPacket.toBytes();
		
		DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, address, port);
		socket.send(ackPacket);
		
		System.out.println("ACK sent. Sequence number " + seqNum);
	}
	
	public static void sendRstPacket(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
		MyPacket myRstPacket = new MyPacket(seqNum, false, false, false, true, new byte[0]);
		byte[] rstBytes = myRstPacket.toBytes();
		
		DatagramPacket rstPacket = new DatagramPacket(rstBytes, rstBytes.length, address, port);
		socket.send(rstPacket);
		
		System.out.println("RST sent. Sequence number " + seqNum);
	}
	
	private static boolean waitForSynAck(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[1024];
		DatagramPacket synAckPacket =  new DatagramPacket(buffer, buffer.length);
		
		socket.receive(synAckPacket);
		MyPacket mySynAckPacket = MyPacket.fromBytes(Arrays.copyOf(buffer, synAckPacket.getLength()));
		
		if (mySynAckPacket.isRst()) {
			System.err.println("RST received during handshake. Aborting.");
			System.exit(1);
		}
		
		return mySynAckPacket.isSyn() && mySynAckPacket.isAck();
	}
	
	private static boolean waitForFinAck(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[1024];
		DatagramPacket finAckPacket =  new DatagramPacket(buffer, buffer.length);
		
		socket.receive(finAckPacket);
		MyPacket myFinAckPacket = MyPacket.fromBytes(Arrays.copyOf(buffer, finAckPacket.getLength()));
		
		if (myFinAckPacket.isRst()) {
			System.err.println("RST received during connection termination. Aborting.");
			System.exit(1);
		}
		
		return myFinAckPacket.isFin() && myFinAckPacket.isAck();
	}
	
	private static void closeConnection(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
		sendFinPacket(socket, address, port, seqNum++);
		if(waitForFinAck(socket)) {
			System.out.println("FIN+ACK received.");
			sendAckPacket(socket, address, port, seqNum++);
			System.out.println("Final ACK sent. Connection ends normally.");
		}
		else {
			System.out.println("Connection failed to end.");
		}
	}

	public static void main(String[] args) {
		
		if (args.length < 3 || args.length > 4) {
		    System.out.println("Usage: java Client <ip> <port> <fichier> [--send-rst]");
		    return;
		}
		
		String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String filePath = args[2];
        boolean sendRst = args.length == 4 && args[3].equals("--send-rst");
        
        try(DatagramSocket socket = new DatagramSocket();
    		FileInputStream file = new FileInputStream(filePath)) {
        				
			InetAddress address = InetAddress.getByName(ip); 		//récupérer IP à partir d'une string
			byte[] buffer = new byte[1024];
			int bytesRead;
			
			//-----------------------------------------------------------------------------------SYN
			
			Random random = new Random();
			int initSeqNum = random.nextInt(65536);
			
			int seqNum = performHandshake(socket, address, port, initSeqNum);
			if(seqNum == -1) {
				System.err.println("Connection failed. Server not reached.");
				return;
			}			
			
			//-----------------------------------------------------------------------------------END HANDSHAKE
			
			if (sendRst) {
                sendRstPacket(socket, address, port, seqNum);
                System.err.println("RST sent intentionally. Aborting.");
                return;
            }
			
			while((bytesRead = file.read(buffer)) != -1) { //-1 == EOF
				
				byte[] byteRead = Arrays.copyOf(buffer, bytesRead);
				
				MyPacket p = new MyPacket(seqNum, false, false, false, false, byteRead);
				byte[] toSend = p.toBytes();
				
				DatagramPacket packet = new DatagramPacket(toSend, toSend.length, address, port);
				System.out.println("Sending packet #" + seqNum + " (" + byteRead.length + " bytes)");

				socket.send(packet);
				seqNum++;
				
			}
			
			System.out.println("File fully sent.");
			
			closeConnection(socket, address, port, seqNum);
						
        }
        catch(IOException e) {
        	
        	System.err.println("Erreur côté client : " + e.getMessage());
            e.printStackTrace();
            
        }

	}

}
