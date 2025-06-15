package Server;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import Shared.MyPacket;

public class Server {
	
	private static void sendAck(DatagramSocket socket, InetAddress address, int port, int seqNum, int ackNum) throws IOException {
		byte[] ackData = new byte[2]; 						//pour stocker les 16 bits du numéro de séquence
		ackData[0] = (byte) ((ackNum >> 8) & 0xFF);			//ackNum = le numéro de séquence du dernier paquet recu de mainere continue 
        ackData[1] = (byte) (ackNum & 0xFF);
        
        MyPacket myAckPacket = new MyPacket(seqNum, false, true, false, false, ackData);
		byte[] ackBytes = myAckPacket.toBytes();
		
		DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, address, port);
		socket.send(ackPacket);
		
		System.out.println("ACK sent. ACK for Sequence number " + ackNum);
	}
	
	private static void sendSynAck(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
		MyPacket mySynAckPacket = new MyPacket(seqNum, true, true, false, false, new byte[0]);
		byte[] synAckBytes = mySynAckPacket.toBytes();
		
		DatagramPacket synAckPacket = new DatagramPacket(synAckBytes, synAckBytes.length, address, port);
		socket.send(synAckPacket);
		
		System.out.println("SYN+ACK sent.");
	}
	
	private static void sendFinAck(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
		MyPacket myFinAckPacket = new MyPacket(seqNum, false, true, true, false, new byte[0]);
		byte[] finAckBytes = myFinAckPacket.toBytes();
		
		DatagramPacket finAckPacket = new DatagramPacket(finAckBytes, finAckBytes.length, address, port);
		socket.send(finAckPacket);
		
		System.out.println("FIN+ACK sent.");
	}
	
	private static void sendRst(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
		MyPacket myRstPacket = new MyPacket(seqNum, false, false, false, true, new byte[0]);
		byte[] rstBytes = myRstPacket.toBytes();
		
		DatagramPacket rstPacket = new DatagramPacket(rstBytes, rstBytes.length, address, port);
		socket.send(rstPacket);
		
		System.out.println("RST sent. ");
    }
	
	private static void handleEOC(DatagramSocket socket, InetAddress addressClient, int portClient, int seqNum) throws IOException {
		System.out.println("FIN received. Sending FIN+ACK...");
		sendFinAck(socket, addressClient, portClient, seqNum);
		
		byte[] buffer = new byte[1024];
	    DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
	    socket.receive(ackPacket);
	    MyPacket ackPAcket = MyPacket.fromBytes(Arrays.copyOf(buffer, ackPacket.getLength()));

	    if (ackPAcket.isAck() && !ackPAcket.isFin() && ackPAcket.getData().length == 0) {
	        System.out.println("Final ACK received. Ending session.");
	    } else {
	        System.err.println("Weird ending : last packet unexpected.");
	    }
	}
	
	private static int findLastContinuousSequenceNumber(TreeMap<Integer, byte[]> dataReceived, int current) {
		 int expected = current;
	        while (dataReceived.containsKey(expected)) {
	            expected++;
	        }
	        return expected - 1;
	}


	public static void main(String[] args) {

		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage : java Server <port> <file>");
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		String outputFile = args[1];
        boolean forceRst = args.length == 3 && args[2].equals("--force-rst");

		
		try(DatagramSocket socket = new DatagramSocket(port);
			FileOutputStream file = new FileOutputStream(outputFile)) {
					
			Map<Integer, byte[]> dataReceived = new TreeMap<>(); 			//dictionnaire trié (ici par numéro de seq)
			byte[] buffer = new byte[2048]; 							//2048 pour avoir une marge de sécurité pour évite le troncage udp lors de problemes
			
			boolean finished = false;
			boolean connected = false;
			boolean firstSynReceived = false;
			
			int lastSeqNumContinuous = -1;
						
			InetAddress clientAddress = null;
			int portClient = -1;
			
			Random random = new Random();
			int serverSeqNum = random.nextInt(65536);
			
			while(!finished) {
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				
				byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
				MyPacket p = MyPacket.fromBytes(data);
				
				clientAddress = packet.getAddress();
				portClient = packet.getPort();
				
				if (forceRst) {
				    sendRst(socket, clientAddress, portClient, serverSeqNum);
				    System.err.println("RST sent to client (forced).");
				    break;
				}
				
				if (p.isRst()) { 
                    System.err.println("RST received from client. Connection ending now.");
                    break;
                }
				
				//----------------------------------------------------------------------------------------------------------------HANDSHAKE
				
				if(!connected) {
					if (p.isSyn() && !firstSynReceived) {
				        System.out.println("SYN received. Sending SYN+ACK...");
				        sendSynAck(socket, clientAddress, portClient, serverSeqNum);
				        firstSynReceived = true;
				    } else if (p.isSyn() && firstSynReceived) {
				        System.out.println("Second SYN received. Connection established.");
				        connected = true;
				        System.out.println("Handshake completed.");
				        lastSeqNumContinuous = p.getSequenceNumber();
				        continue;
				    } else {
				        System.out.println("Packet ignored. Handshake failed.");
				        continue;
				    }
				}
				//--------------------------------------------------------------------------------------------------------------------
				if (!p.isFin()) {
		
				    dataReceived.put(p.getSequenceNumber(), p.getData());
				    System.out.println("Packet received. SeqNum " + p.getSequenceNumber());

				    int newLastSeq = findLastContinuousSequenceNumber((TreeMap<Integer, byte[]>) dataReceived, lastSeqNumContinuous + 1);
				    if (newLastSeq != lastSeqNumContinuous) {
				        lastSeqNumContinuous = newLastSeq;
				        sendAck(socket, clientAddress, portClient, serverSeqNum++, lastSeqNumContinuous);
				    }
				    continue;
				}
				
				if (p.isFin() && p.getData().length == 0) {
				    handleEOC(socket, clientAddress, portClient, serverSeqNum);
				    finished = true;
				    continue;
				}
			}
			System.out.println("Total packets received : " + dataReceived.size());

				for(byte[] bloc : dataReceived.values()) {
					file.write(bloc);
				}
				
				System.out.println("File stuffed.");
				
			}
			
		catch(IOException e) {
			
			System.err.println("Server error : " + e.getMessage());
            e.printStackTrace();
            
		}
		
	}

}
