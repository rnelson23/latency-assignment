package server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class TCP {
    public static class Packet {
        public byte[] data;
        public byte[] raw;

        public Packet(int size) {
            this.data = new byte[size];
            this.raw = new byte[8];
        }

        public void decode() {
            for (int i = 0; i < raw.length; i++) {
                data[i] = (byte) (raw[i] ^ (Long.MAX_VALUE >> (i * 8)));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket socket = new ServerSocket(42070)) {
            Socket client = socket.accept();

            OutputStream out = client.getOutputStream();
            InputStream in = client.getInputStream();

            while (true) {
                byte[] header = new byte[4];
                int headerBytes = in.read(header);
                int size = ByteBuffer.wrap(header).getInt();

                if (headerBytes == -1) { continue; }
                if (size == -3) { break; }

                while (true) {
                    Packet packet = new Packet(size);
                    int dataBytes = in.read(packet.data);

                    if (dataBytes == -1) { continue; }
                    if (ByteBuffer.wrap(packet.data).getInt() == -2) { break; }

                    packet.decode();
                    out.write(packet.raw);
                }
            }
        }
    }
}
