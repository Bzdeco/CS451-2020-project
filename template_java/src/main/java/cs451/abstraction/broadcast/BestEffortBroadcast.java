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

    public BestEffortBroadcast(int hostId, List<Host> allHosts, PayloadFactory payloadFactory) {
        super();
        HostResolver hostResolver = new HostResolver(allHosts);
        Host host = hostResolver.getHostById(hostId);

        this.hostId = hostId;
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

    public void broadcast(Payload payload, boolean isRelay) {
        sendToOtherHosts(payload, isRelay);
        // message might not have been physically sent, but it's queued for sending and guaranteed to be sent by PL
        if (!isRelay) emitBroadcastEvent(payload);
        sendToMyself(payload); // simply delivers the message to the broadcasting host
    }

    private void sendToOtherHosts(Payload payload, boolean isBroadcastThroughQueue) {
        otherHosts.forEach(receiver -> {
            Message message = messageFactory.createMessageWithPayload(hostId, receiver.getId(), payload);
            if (!isBroadcastThroughQueue) perfectLink.send(message);
            else perfectLink.queueForSending(message); // to keep sending in one thread
        });
    }

    private void sendToMyself(Payload payload) {
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
