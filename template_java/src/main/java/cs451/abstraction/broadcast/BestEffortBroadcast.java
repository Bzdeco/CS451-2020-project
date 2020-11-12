package cs451.abstraction.broadcast;

import cs451.abstraction.Notifier;
import cs451.abstraction.Observer;
import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.PerfectLink;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.MessageFactory;
import cs451.parser.Host;

import java.util.*;
import java.util.stream.IntStream;

public class BestEffortBroadcast extends Notifier implements Observer {

    final private int hostId;
    final private List<Host> otherHosts;
    final private MessageFactory messageFactory;
    final private PerfectLink perfectLink;

    public BestEffortBroadcast(int hostId, List<Host> allHosts, HostResolver hostResolver) {
        super();
        this.hostId = hostId;
        Host host = hostResolver.getHostById(hostId);
        this.otherHosts = createListOfOtherHosts(host, allHosts);
        this.messageFactory = new MessageFactory(hostResolver);

        this.perfectLink = new PerfectLink(host, allHosts, messageFactory); // FIXME can reuse factory?
        perfectLink.registerDeliveryObserver(this);
        perfectLink.startThreads();
    }

    private static List<Host> createListOfOtherHosts(Host myself, List<Host> allHosts) {
        List<Host> hosts = new LinkedList<>(allHosts);
        hosts.remove(myself);
        return hosts;
    }

    // TODO: should be given data to broadcast
    public void broadcast(int sequenceNumber) {
        sendToOtherHosts(sequenceNumber);
        emitBroadcastEvent(sequenceNumber);
        sendToMyself(sequenceNumber); // will simply deliver the message to the broadcasting host
    }

    private void sendToOtherHosts(int sequenceNumber) {
        otherHosts.forEach(receiver -> {
            Message message = messageFactory.createPayloadMessage(hostId, receiver.getId(), sequenceNumber);
            perfectLink.send(message);
        });
    }

    private void sendToMyself(int sequenceNumber) {
        Message selfMessage = messageFactory.createPayloadMessage(hostId, hostId, sequenceNumber);
        notifyOfDelivery(selfMessage);
    }

    @Override
    public void notifyOfDelivery(Message message) {
        emitDeliverEvent(message);
    }

    public void stop() {
        perfectLink.stopThreads();
    }
}
