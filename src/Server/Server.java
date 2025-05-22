package Server;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import Shared.MyPacket;

public class Server {

	public static void main(String[] args) {

		if(args.length != 2) {
			System.out.println("Usage : java Server <port> <file>");
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		String outputFile = args[1];
		
		try(DatagramSocket socket = new DatagramSocket(port);
			FileOutputStream file = new FileOutputStream(outputFile)) {
					
			Map<Integer, byte[]> dataReceived = new TreeMap(); 			//dictionnaire trié (ici par numéro de seq)
			byte[] buffer = new byte[2048]; 							//2048 pour avoir une marge de sécurité pour évite le troncage udp lors de problemes
			boolean fin = false;
			
			while(!fin) {
				
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				
				byte[] rawData = Arrays.copyOf(packet.getData(), packet.getLength());
				MyPacket p = MyPacket.fromBytes(rawData);
				dataReceived.put(p.getSequenceNumber(), p.getData());
				
				System.out.println("Reçu paquet #" + p.getSequenceNumber() + " (" + p.getData().length + " octets)");

				if(p.isFin()) {
					fin = true;
					System.out.println("FIN received. File in construction...");
				}
			}
			System.out.println("Nombre total de paquets reçus : " + dataReceived.size());

				for(byte[] bloc : dataReceived.values()) {
					file.write(bloc);
				}
				
				System.out.println("File filled.");
				
			}
			
		catch(IOException e) {
			
			System.err.println("Erreur côté serveur : " + e.getMessage());
            e.printStackTrace();
            
		}
		
	}

}
