package server;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

public class UDP {
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
        try (DatagramSocket socket = new DatagramSocket(42069)) {
            while (true) {
                byte[] header = new byte[4];
                socket.receive(new DatagramPacket(header, header.length));

                int size = ByteBuffer.wrap(header).getInt();
                if (size == -3) { break; }

                while (true) {
                    Packet packet = new Packet(size);
                    DatagramPacket datagramPacket = new DatagramPacket(packet.data, packet.data.length);
                    socket.receive(datagramPacket);

                    if (ByteBuffer.wrap(packet.data).getInt() == -2) { break; }
                    packet.decode();

                    socket.send(new DatagramPacket(packet.raw, packet.raw.length, datagramPacket.getAddress(), datagramPacket.getPort()));
                }
            }
        }
    }
}
