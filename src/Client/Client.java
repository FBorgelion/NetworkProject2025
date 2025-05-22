package Client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import Shared.MyPacket;

public class Client {

	public static void main(String[] args) {
		
		if (args.length != 3) {
            System.out.println("Usage: java Client <ip> <port> <fichier>");
            return;
        }
		
		String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String filePath = args[2];
        
        try(DatagramSocket socket = new DatagramSocket();
    		FileInputStream file = new FileInputStream(filePath)) {
        				
			InetAddress address = InetAddress.getByName(ip); 		//récupérer IP à partir d'une string
			byte[] buffer = new byte[1024];
			int bytesRead;
			int seqNum = 0;
			
			while((bytesRead = file.read(buffer)) != -1) { //-1 == EOF
				
				byte[] byteRead = Arrays.copyOf(buffer, bytesRead);
				boolean isLast = file.available() == 0;		 		//compte les bytes restants
				
				MyPacket p = new MyPacket(seqNum, false, false, isLast, false, byteRead);
				byte[] toSend = p.toBytes();
				
				DatagramPacket packet = new DatagramPacket(toSend, toSend.length, address, port);
				System.out.println("Envoi paquet #" + seqNum + " (" + byteRead.length + " octets)");

				socket.send(packet);
				seqNum++;
				
			}
			
			System.out.println("File fully sent.");
						
        }
        catch(IOException e) {
        	
        	System.err.println("Erreur côté client : " + e.getMessage());
            e.printStackTrace();
            
        }

	}

}
