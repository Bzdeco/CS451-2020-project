package cs451.abstraction.link;

import cs451.abstraction.Notifier;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class Receiver extends Notifier {

    final private static int MAX_BYTES_IN_PACKET = 256; // FIXME: arbitrary

    final private DatagramSocket receivingSocket;
    final private MessagesStorage storage;
    final private DatagramDataFactory datagramDataFactory;
    final private MessageFactory messageFactory;

    public Receiver(Host host, MessagesStorage storage, PayloadFactory payloadFactory,
                    MessageFactory messageFactory) {
        super();
        this.storage = storage;
        this.receivingSocket = createReceivingSocket(host);
        this.datagramDataFactory = new DatagramDataFactory(payloadFactory);
        this.messageFactory = messageFactory;
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

    public void receive() {
        DatagramPacket receivedPacket = doReceive();
        DatagramData data = datagramDataFactory.create(receivedPacket);
        storage.addReceivedData(data);
    }

    public void processReceivedPackets() {
        Set<DatagramData> toRemoveFromReceived = new HashSet<>();

        storage.getReceivedData().forEach(data -> {
            DatagramDataType dataType = data.getDataType();

            if (dataType.equals(DatagramDataType.PAYLOAD)) {
                queueAcknowledgmentReply(data);
                emitDeliverEvent(messageFactory.createReceived(data));
                toRemoveFromReceived.add(data);
            } else if (dataType.equals(DatagramDataType.ACK)) {
                boolean acknowledged = acknowledge(data);
                if (acknowledged) toRemoveFromReceived.add(data);
            }
        });

        storage.removeFromReceivedData(toRemoveFromReceived);
    }

    private void queueAcknowledgmentReply(DatagramData data) {
        DatagramData ackData = DatagramData.convertReceivedToAcknowledgment(data);
        Message ackReply = messageFactory.createToSend(ackData);
        storage.addAcknowledgmentToSend(ackReply);
    }

    private boolean acknowledge(DatagramData ackData) {
        DatagramData originalData = DatagramData.convertAcknowledgmentToOriginal(ackData);
        Message originalMessage = messageFactory.createToSend(originalData);
        return storage.acknowledge(originalMessage, ackData);
    }

    private DatagramPacket doReceive() {
        try {
            byte[] buffer = new byte[MAX_BYTES_IN_PACKET];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            receivingSocket.receive(packet);
            return packet;
        } catch (IOException exc) {
            System.err.println("Unable to receive a UDP packet due to I/O exception");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }
}
