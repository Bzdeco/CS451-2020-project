package cs451.abstraction.broadcast;

import cs451.abstraction.Notifier;
import cs451.abstraction.Observer;
import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.PerfectLink;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.*;

public class BestEffortBroadcast extends Notifier implements Observer {

    final private int hostId;
    final private List<Host> otherHosts;
    final private MessageFactory messageFactory;
    final private PerfectLink perfectLink;

    public BestEffortBroadcast(int hostId, List<Host> allHosts, HostResolver hostResolver, PayloadFactory payloadFactory) {
        super();
        this.hostId = hostId;
        Host host = hostResolver.getHostById(hostId);
        this.otherHosts = createListOfOtherHosts(host, allHosts);
        this.messageFactory = new MessageFactory(hostResolver);

        this.perfectLink = new PerfectLink(host, allHosts, payloadFactory, messageFactory); // FIXME can reuse factory?
        perfectLink.registerDeliveryObserver(this);
        perfectLink.startThreads();
    }

    private static List<Host> createListOfOtherHosts(Host myself, List<Host> allHosts) {
        List<Host> hosts = new LinkedList<>(allHosts);
        hosts.remove(myself);
        return hosts;
    }

    public void broadcast(URBPayload payload) {
        sendToOtherHosts(payload);
        emitBroadcastEvent(payload);
        sendToMyself(payload); // simply delivers the message to the broadcasting host
    }

    private void sendToOtherHosts(URBPayload payload) {
        otherHosts.forEach(receiver -> {
            Message message = messageFactory.createMessageWithPayload(hostId, receiver.getId(), payload);
            perfectLink.send(message);
        });
    }

    private void sendToMyself(URBPayload payload) {
        Message selfMessage = messageFactory.createMessageWithPayload(hostId, hostId, payload);
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