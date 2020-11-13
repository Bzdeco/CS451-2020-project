package cs451.abstraction.link.message;

import cs451.abstraction.link.HostResolver;
import cs451.parser.Host;

public class MessageFactory {

    final private HostResolver hostResolver;

    public MessageFactory(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    public Message createMessageWithPayload(int senderHostId, int receiverHostId, Payload payload) {
        DatagramData data = new DatagramData(senderHostId, receiverHostId, DatagramDataType.PAYLOAD, payload);
        return createToSend(data);
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
