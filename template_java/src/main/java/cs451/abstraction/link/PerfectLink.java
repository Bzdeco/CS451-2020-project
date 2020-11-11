package cs451.abstraction.link;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.MessageFactory;
import cs451.parser.Host;

import java.util.List;

public class PerfectLink implements Link {

    final private Sender sender;
    final private Receiver receiver;

    public PerfectLink(Host host, List<Host> allHosts, MessageFactory messageFactory) {
        MessagesStorage storage = new MessagesStorage(allHosts);
        HostResolver hostResolver = new HostResolver(allHosts);

        this.sender = new Sender(storage);
        this.receiver = new Receiver(host, storage, hostResolver, messageFactory);
    }


    @Override
    public void send(Host receiver, DatagramData data) {

    }

    @Override
    public void deliver(Message message) {

    }
}
