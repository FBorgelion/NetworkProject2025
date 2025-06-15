package Client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import Shared.MyPacket;

public class Client {
	
	private static final int WINDOW_SIZE = 256;
	private static final int MAX_SEQ_NUM = 1 << 16;
	
	private static int performHandshake(DatagramSocket socket, InetAddress address, int port, int seqNum) throws IOException {
        for (int i = 0; i < 3; i++) {
            sendSynPacket(socket, address, port, seqNum);
            System.out.println("Try " + (i + 1));
            if (waitForSynAck(socket)) {
            	System.out.println("SYN+ACK received");
            	seqNum = (seqNum + 1) % MAX_SEQ_NUM;
            	sendSynPacket(socket, address, port, seqNum);
                System.out.println("Handshake completed.");
                return (seqNum + 1) % MAX_SEQ_NUM;
            }
			System.err.println("No response. Try " + (i + 1));
        }
        return -1;
    }
	
	private static boolean isSeqNumLE(int s, int ack) {							//check si la taille max a été depassée --> retour à 0
		return ((ack - s + MAX_SEQ_NUM) % MAX_SEQ_NUM) < (MAX_SEQ_NUM / 2);
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
	
	private static int waitForAck(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[1024];
		DatagramPacket ackPacket = new DatagramPacket(buffer, buffer.length);
		socket.setSoTimeout(500);
		try {
			socket.receive(ackPacket);
			MyPacket myAckPacket = MyPacket.fromBytes(Arrays.copyOf(buffer, ackPacket.getLength()));
			if (myAckPacket.isAck() && myAckPacket.getData().length == 2) {
				int ackNum = ((myAckPacket.getData()[0] & 0xFF) << 8) | (myAckPacket.getData()[1] & 0xFF);
				System.out.println("ACK received for seqNum " + ackNum);
				return ackNum;
			}
		} catch (SocketTimeoutException e) {
			System.out.println("ACK wait timed out.");
		}
		return -1;
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

	public static void main(String[] args) throws InterruptedException {
		
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
			
			Map<Integer, MyPacket> bufferWindow = new TreeMap<>();
			
			List<Integer> ackNums = new ArrayList<>();
			List<Integer> ackPacketSeqs = new ArrayList<>();
		    int lastAckPacketSeqNum = 0;

			
			int baseSeq = seqNum;
			
			boolean fileDone = false;

			while((bytesRead = file.read(buffer)) != -1 || !bufferWindow.isEmpty()) { //-1 == EOF
				if((bytesRead != -1) && ((seqNum-baseSeq + MAX_SEQ_NUM) % MAX_SEQ_NUM < WINDOW_SIZE)) {	//vérifie qu'il reste des donnees à lire && vérifie que les numéros de séquence sont dans la fenetre de transmission
					
					byte[] byteRead = Arrays.copyOf(buffer, bytesRead);
					
					MyPacket p = new MyPacket(seqNum, false, false, false, false, byteRead);
					byte[] toSend = p.toBytes();
					
					bufferWindow.put(seqNum, p);
					
					DatagramPacket packet = new DatagramPacket(toSend, toSend.length, address, port);
					System.out.println("Sending packet " + seqNum + " (" + byteRead.length + " bytes)");
	
					socket.send(packet);
					seqNum++;
					
				}
			

			    int ack = waitForAck(socket);
				if(ack != -1) {				
					int ackPacketSeq = lastAckPacketSeqNum++;
					ackNums.add(ack);
					ackPacketSeqs.add(ackPacketSeq);
					
					if(ackNums.size() > 3) {
						ackNums.remove(0);
					}
					if (ackPacketSeqs.size() > 3) {
						ackPacketSeqs.remove(0);
					}
					
					boolean sameAck = ackNums.size() == 3 && ackNums.get(0).equals(ackNums.get(1)) && ackNums.get(1).equals(ackNums.get(2));

					boolean distinctPackets = ackPacketSeqs.size() == 3 && ackPacketSeqs.stream().distinct().count() == 3;
					
					bufferWindow.keySet().removeIf(s -> isSeqNumLE(s, ack));	//tous les paquets dans bufferWindow dont le numéro de séquence s est inférieur ou égal à ack (modulo 65536) sont supprimés.
					baseSeq = (ack + 1) % MAX_SEQ_NUM;
					
					if (sameAck && distinctPackets) {
				        System.out.println("Triple ACK identical. Retransmitting....");
				        for (MyPacket p : bufferWindow.values()) {
				            socket.send(new DatagramPacket(p.toBytes(), p.toBytes().length, address, port));
				        }
				        ackNums.clear();
				        ackPacketSeqs.clear();
				    }
				}
				
			}
			
			long lastAckTime = System.currentTimeMillis();
			long period = 3000; // 3 secondes pour laisser le server le temps de traiter les données

			while (!bufferWindow.isEmpty() && System.currentTimeMillis() - lastAckTime < period) {
			    int ack = waitForAck(socket);
			    if (ack != -1) {
			        lastAckTime = System.currentTimeMillis(); // reset timer
			        
			        int ackPacketSeq = lastAckPacketSeqNum++;

			        ackNums.add(ack);
			        ackPacketSeqs.add(ackPacketSeq);
			        
			        if (ackNums.size() > 3) {
			        	ackNums.remove(0);
			        }
			        if (ackPacketSeqs.size() > 3) {
			        	ackPacketSeqs.remove(0);
			        }

			        bufferWindow.keySet().removeIf(s -> isSeqNumLE(s, ack));
			        baseSeq = (ack + 1) % MAX_SEQ_NUM;
			        
			        boolean sameAck = ackNums.size() == 3 && ackNums.get(0).equals(ackNums.get(1)) && ackNums.get(1).equals(ackNums.get(2));

			        boolean distinctPackets = ackPacketSeqs.size() == 3 && ackPacketSeqs.stream().distinct().count() == 3;

			        if (sameAck && distinctPackets) {
				        System.out.println("Triple ACK identical. Retransmitting....");
			            for (MyPacket p : bufferWindow.values()) {
			                socket.send(new DatagramPacket(p.toBytes(), p.toBytes().length, address, port));
			            }
			            ackNums.clear();
			            ackPacketSeqs.clear();
			        }
			    }
			}

			
			System.out.println("File fully sent.");
			
			closeConnection(socket, address, port, seqNum);
			
			
						
        }
        catch(IOException e) {
        	
        	System.err.println("Error client : " + e.getMessage());
            e.printStackTrace();
            
        }

	}

}
