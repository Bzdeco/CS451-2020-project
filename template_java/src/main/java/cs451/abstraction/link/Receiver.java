package cs451.abstraction.link;

import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.MessageFactory;
import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class Receiver {

    final private static int MAX_BYTES_IN_PACKET = 256; // FIXME: arbitrary

    final private DatagramSocket receivingSocket;
    final private SentMessagesStorage storage;
    final private HostResolver hostResolver;
    final private MessageFactory messageFactory;

    private byte[] receiveBuffer; // TODO: can reuse?

    public Receiver(Host host, SentMessagesStorage storage, HostResolver hostResolver, MessageFactory messageFactory) {
        this.storage = storage;
        this.receivingSocket = createReceivingSocket(host);
        this.hostResolver = hostResolver;
        this.messageFactory = messageFactory;
        this.receiveBuffer = new byte[MAX_BYTES_IN_PACKET];
    }

    private DatagramSocket createReceivingSocket(Host host) {
        InetSocketAddress socketAddress = new InetSocketAddress(host.getIp(), host.getPort());
        try {
            return new DatagramSocket(socketAddress);
        } catch (SocketException exc) {
            System.err.println("Unable to create receiving UDP socket");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    public Message receive() {
        DatagramPacket receivedPacket = doReceive();
        return messageFactory.createReceived(receivedPacket);
        // TODO: check if with data or ack message
        // if with data order sender to send ack message and deliver this one (?)
        // if ack message process recent unacknowledged and stale messages in storage
    }

    private DatagramPacket doReceive() {
        try {
            DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            receivingSocket.receive(packet);
            return packet;
        } catch (IOException exc) {
            System.err.println("Unable to receive a UDP packet due to I/O exception");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }
}
