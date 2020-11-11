package cs451.abstraction.link.message;

import cs451.abstraction.link.HostResolver;
import cs451.parser.Host;

import java.net.DatagramPacket;

public class MessageFactory {

    final private HostResolver hostResolver;

    public MessageFactory(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    public Message createToSend(DatagramData data) {
        Host receiver = hostResolver.resolveReceiverHost(data);
        return new Message(null, receiver, data);
    }

    public Message createReceived(DatagramData data) {
        Host sender = hostResolver.resolveSenderHost(data);
        return new Message(sender, null, data);
    }
}
