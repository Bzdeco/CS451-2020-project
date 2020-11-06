package cs451.abstraction.link;

import cs451.parser.Host;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Optional;

public class Message implements Serializable {

    final private Optional<Host> sender;
    final private Optional<Host> receiver;
    final private Optional<DatagramData> data;

    private Optional<MessageTransmissionProperties> properties;

    /**
     * Creates a message dedicated to be delivered to the specified receiver host.
     *
     * @param receiver host to which the message should be delivered
     * @param data message content
     */
    public Message(Host receiver, DatagramData data) {
        this.sender = Optional.empty();
        this.receiver = Optional.of(receiver);
        this.data = Optional.of(data);
        this.properties = Optional.empty();
    }

    /**
     * Creates a received message based on the received UDP packet.
     *
     * @param udpPacket received UDP packet containing message data
     * @param hostResolver utility object decoding the sender identity from the packet data
     * @param dataFactory utility object producing concrete type of the message data decoded from the packet
     */
    public Message(DatagramPacket udpPacket, HostResolver hostResolver, DatagramDataFactory dataFactory) {
        this.sender = Optional.of(hostResolver.resolveSenderHost(udpPacket));
        this.receiver = Optional.empty();
        this.data = Optional.of(dataFactory.from(udpPacket));
        this.properties = Optional.empty();
    }

    public Host getReceiver() {
        if (receiver.isEmpty()) {
            throw new RuntimeException("Accessed empty receiver field");
        }
        return receiver.get();
    }

    public Host getSender() {
        if (sender.isEmpty()) {
            throw new RuntimeException("Accessed empty sender field");
        }
        return sender.get();
    }

    public DatagramData getData() {
        if (data.isEmpty()) {
            throw new RuntimeException("Accessed empty data field");
        }
        return data.get();
    }

    public MessageTransmissionProperties getTransmissionProperties() {
        if (properties.isEmpty()) {
            throw new RuntimeException("Accessed empty transmission properties field");
        }
        return properties.get();
    }

    public DatagramPacket toSentPacket() {
        checkRequiredFields();

        byte[] messageBytes = data.get().convertToBytes();
        Host receiverHost = receiver.get();
        InetSocketAddress receiverSocketAddress = new InetSocketAddress(receiverHost.getIp(), receiverHost.getPort());
        return new DatagramPacket(messageBytes, messageBytes.length, receiverSocketAddress);
    }

    private void checkRequiredFields() {
        if (receiver.isEmpty() || data.isEmpty()) {
            throw new RuntimeException("Sent message missing a receiver and/or data");
        }
    }

    public void markSending() {
        properties.ifPresentOrElse(MessageTransmissionProperties::markSending, this::initializeProperties);
    }

    private void initializeProperties() {
        this.properties = Optional.of(new MessageTransmissionProperties());
    }
}
