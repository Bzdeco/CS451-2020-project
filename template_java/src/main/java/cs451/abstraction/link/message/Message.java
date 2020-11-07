package cs451.abstraction.link.message;

import cs451.parser.Host;

import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class Message implements Serializable {

    final private Host sender;
    final private Host receiver;
    final private DatagramData data;
    private MessageTransmissionProperties properties;

    Message(Host sender, Host receiver, DatagramData data, MessageTransmissionProperties properties) {
        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
        this.properties = properties;
    }

    public Host getReceiver() {
        if (receiver == null) {
            throw new RuntimeException("Accessed empty receiver field");
        }
        return receiver;
    }

    public Host getSender() {
        if (sender == null) {
            throw new RuntimeException("Accessed empty sender field");
        }
        return sender;
    }

    public DatagramData getData() {
        if (data == null) {
            throw new RuntimeException("Accessed empty data field");
        }
        return data;
    }

    public MessageTransmissionProperties getTransmissionProperties() {
        if (properties == null) {
            throw new RuntimeException("Accessed empty transmission properties field");
        }
        return properties;
    }

    public DatagramPacket toSentPacket() {
        byte[] messageBytes = getData().convertToBytes();
        Host receiver = getReceiver();
        InetSocketAddress receiverSocketAddress = new InetSocketAddress(receiver.getIp(), receiver.getPort());
        return new DatagramPacket(messageBytes, messageBytes.length, receiverSocketAddress);
    }

    public void markSending() {
        if (properties == null) {
            initializeProperties();
        } else {
            properties.markSending();
        }
    }

    private void initializeProperties() {
        this.properties = new MessageTransmissionProperties();
    }
}
