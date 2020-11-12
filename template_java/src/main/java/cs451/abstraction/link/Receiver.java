package cs451.abstraction.link;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.DatagramDataType;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.MessageFactory;
import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Receiver {

    final private static int MAX_BYTES_IN_PACKET = 256; // FIXME: arbitrary

    final private DatagramSocket receivingSocket;
    final private MessagesStorage storage;
    final private HostResolver hostResolver;
    final private MessageFactory messageFactory;
    final private List<DeliveryObserver> deliveryObservers;

    public Receiver(Host host, MessagesStorage storage, HostResolver hostResolver, MessageFactory messageFactory) {
        this.storage = storage;
        this.receivingSocket = createReceivingSocket(host);
        this.hostResolver = hostResolver;
        this.messageFactory = messageFactory;
        this.deliveryObservers = new LinkedList<>();
    }

    public void registerDeliveryObserver(DeliveryObserver observer) {
        deliveryObservers.add(observer);
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
        DatagramData data = new DatagramData(receivedPacket);
        storage.addReceivedData(data);
    }

    public void processReceivedPackets() {
        Set<DatagramData> toRemoveFromReceived = new HashSet<>();

        storage.getReceivedData().forEach(data -> {
            DatagramDataType dataType = data.getDataType();

            if (dataType.equals(DatagramDataType.PAYLOAD)) {
                queueAcknowledgmentReply(data);
                deliver(messageFactory.createReceived(data));
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

    private void deliver(Message message) {
        deliveryObservers.forEach(observer -> observer.notifyOfDelivery(message));
    }
}
