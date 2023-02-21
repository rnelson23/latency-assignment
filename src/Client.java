import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Client {
    public static class Packet {
        public byte[] raw;
        public byte[] data;
        public byte[] ack;

        public Packet(int size) {
            this.raw = new byte[size];
            this.data = new byte[size];
            this.ack = new byte[8];

            for (int i = 0; i < size; i++) {
                data[i] = (byte) (raw[i] ^ (Long.MAX_VALUE >> (i * 8)));
            }
        }
    }

    public static int port = 42069;
    public static InetAddress address;
    public static DatagramSocket socket;
    public static OutputStream out;
    public static InputStream in;

    public static void main(String[] args) throws IOException {
        String host = args[0] + ".cs.oswego.edu";

        try (Socket socket = new Socket(host, port + 1)) {
            out = socket.getOutputStream();
            in = socket.getInputStream();

            System.out.println("\nTCP Round Trip Times: ");
            System.out.println("8 bytes: " + round(timeTCP(1, 8)) + " ms");
            System.out.println("32 bytes: " + round(timeTCP(1, 32)) + " ms");
            System.out.println("512 bytes: " + round(timeTCP(1, 512)) + " ms");
            System.out.println("1024 bytes: " + round(timeTCP(1, 1024)) + " ms");

            System.out.println("\nTCP Throughput: ");
            System.out.println("1024 x 1024 bytes: " + toMbps(timeTCP(1024, 1024)) + " Mbps");
            System.out.println("2048 x 512 bytes: " + toMbps(timeTCP(2048, 512)) + " Mbps");
            System.out.println("8192 x 128 bytes: " + toMbps(timeTCP(8192, 128)) + " Mbps");

            out.write(ByteBuffer.allocate(4).putInt(-3).array());
        }

        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            address = InetAddress.getByName(host);
            socket = datagramSocket;

            System.out.println("\nUDP Round Trip Times: ");
            System.out.println("8 bytes: " + round(timeUDP(1, 8)) + " ms");
            System.out.println("32 bytes: " + round(timeUDP(1, 32)) + " ms");
            System.out.println("512 bytes: " + round(timeUDP(1, 512)) + " ms");
            System.out.println("1024 bytes: " + round(timeUDP(1, 1024)) + " ms");

            System.out.println("\nUDP Throughput: ");
            System.out.println("1024 x 1024 bytes: " + toMbps(timeUDP(1024, 1024)) + " Mbps");
            System.out.println("2048 x 512 bytes: " + toMbps(timeUDP(2048, 512)) + " Mbps");
            System.out.println("8192 x 128 bytes: " + toMbps(timeUDP(8192, 128)) + " Mbps");

            socket.send(new DatagramPacket(ByteBuffer.allocate(4).putInt(-3).array(), 4, address, port));
        }
    }

    public static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    public static double toMbps(double value) {
        return round(8 / (value / 1000D));
    }

    public static double timeTCP(int count, int size) throws IOException {
        Packet packet = new Packet(size);

        out.write(ByteBuffer.allocate(4).putInt(size).array());
        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            out.write(packet.data);
            int bytes = in.read(packet.ack);

            if (bytes == -1) { return -1; }
            for (int j = 0; j < packet.ack.length; j++) {
                if (packet.ack[j] != packet.raw[j]) {
                    return -1;
                }
            }
        }

        long end = System.nanoTime();
        out.write(ByteBuffer.allocate(4).putInt(-2).array());

        return (end - start) / 1000000D;
    }

    public static double timeUDP(int count, int size) throws IOException {
        Packet packet = new Packet(size);

        socket.send(new DatagramPacket(ByteBuffer.allocate(4).putInt(size).array(), 4, address, port));
        long start = System.nanoTime();

        for (int i = 0; i < count; i++) {
            socket.send(new DatagramPacket(packet.data, packet.data.length, address, port));
            socket.receive(new DatagramPacket(packet.ack, packet.ack.length));

            for (int j = 0; j < packet.ack.length; j++) {
                if (packet.ack[j] != packet.raw[j]) {
                    return -1;
                }
            }
        }

        long end = System.nanoTime();
        socket.send(new DatagramPacket(ByteBuffer.allocate(4).putInt(-2).array(), 4, address, port));

        return (end - start) / 1000000D;
    }
}
