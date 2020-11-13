package cs451.abstraction.link.message;

import cs451.parser.Host;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Objects;

public class Message {

    final private Host sender;
    final private Host receiver;
    final private DatagramData data;

    Message(Host sender, Host receiver, DatagramData data) {
        this.sender = sender;
        this.receiver = receiver;
        this.data = data;
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

    public Payload getPayload() {
        return getData().getPayload();
    }

    public DatagramPacket toSentPacket() {
        byte[] messageBytes = getData().convertToBytes();
        Host receiver = getReceiver();
        InetSocketAddress receiverSocketAddress = new InetSocketAddress(receiver.getIp(), receiver.getPort());
        return new DatagramPacket(messageBytes, messageBytes.length, receiverSocketAddress);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return data.equals(message.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
