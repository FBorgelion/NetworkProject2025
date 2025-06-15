package Shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MyPacket {
		
		private final int dataLength;
		private final int sequenceNumber;
		private final boolean syn;
		private final boolean ack;
		private final boolean fin;
		private final boolean rst;
		private final byte[] data;
		
		public MyPacket(int sequenceNumber, boolean syn, boolean ack, boolean fin, boolean rst, byte[] data) {
			this.data = (data != null) ? data : new byte[0];
			this.dataLength = this.data.length;
			this.sequenceNumber = sequenceNumber;
			this.syn = syn;
			this.ack = ack; 
			this.fin = fin;
			this.rst = rst;
		}
		
		public byte[] toBytes() throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			out.write((dataLength >> 8) & 0xFF); 
			out.write(dataLength & 0xFF);
			
			out.write((sequenceNumber >> 8) & 0xFF); 
			out.write(sequenceNumber & 0xFF);
			
			int flags = 0;
			if(syn) flags |= 0b0001; 
			if(ack) flags |= 0b0010;
			if(fin) flags |= 0b0100;
			if(rst) flags |= 0b1000;
			out.write(flags);
			
			out.write(data);
			
			return out.toByteArray();
		}
		
		public static MyPacket fromBytes(byte[] bytes) {
			int dataLength = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF); //0xFF force le byte a être non-signé
			int seqNum = ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
			int flags = bytes[4] & 0xFF;

			boolean syn = (flags & 0b0001) != 0;
			boolean ack = (flags & 0b0010) != 0;
			boolean fin = (flags & 0b0100) != 0;
			boolean rst = (flags & 0b1000) != 0;
			
			byte[] data = Arrays.copyOfRange(bytes, 5, 5 + dataLength); //copie les données en retirant l'en tête
			
			return new MyPacket(seqNum, syn, ack, fin, rst, data);
		}
		

		public int getDataLength() {
			return dataLength;
		}

		public int getSequenceNumber() {
			return sequenceNumber;
		}

		public boolean isSyn() {
			return syn;
		}

		public boolean isAck() {
			return ack;
		}

		public boolean isFin() {
			return fin;
		}

		public boolean isRst() {
			return rst;
		}
		
		public byte[] getData() {
			return data;
		}

}
