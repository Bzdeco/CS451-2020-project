package cs451.abstraction.link;

import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class PerfectLink implements Link {

    final private Host host;
    final private DatagramSocket sendingSocket;
    final private DatagramSocket receivingSocket;

    public PerfectLink(Host host) {
        this.host = host;
        this.sendingSocket = createSendingSocket();
        this.receivingSocket = createReceivingSocket(host);
    }

    private DatagramSocket createSendingSocket() {
        try {
            return new DatagramSocket();
        } catch (SocketException exc) {
            System.err.println("Unable to create sending UDP socket");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    private DatagramSocket createReceivingSocket(Host host) {
        try {
            return new DatagramSocket(new InetSocketAddress(host.getIp(), host.getPort()));
        } catch (SocketException exc) {
            System.err.println("Unable to create receiving UDP socket for host " + host.getId());
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    @Override
    public void send(Host receiver, DatagramData data) {
        Message message = new Message(receiver, data);
        try {
            sendingSocket.send(message.toSentPacket());
        } catch (IOException exc) {
            System.err.println("Unable to send UDP packet due to I/O exception");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    @Override
    public void deliver(Message message) {

    }
}
