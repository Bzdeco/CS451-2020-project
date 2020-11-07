package cs451.abstraction.link.message;

import cs451.abstraction.link.HostResolver;
import cs451.parser.Host;

import java.net.DatagramPacket;

public class MessageFactory {

    final private HostResolver hostResolver;
    final private DatagramDataFactory dataFactory;

    public MessageFactory(HostResolver hostResolver, DatagramDataFactory dataFactory) {
        this.hostResolver = hostResolver;
        this.dataFactory = dataFactory;
    }

    public Message createToSend(Host receiver, DatagramData data) {
        return new Message(null, receiver, data, null);
    }

    public Message createReceived(DatagramPacket udpPacket) {
        Host sender = hostResolver.resolveSenderHost(udpPacket);
        DatagramData data = dataFactory.from(udpPacket);
        return new Message(sender, null, data, null);
    }
}
