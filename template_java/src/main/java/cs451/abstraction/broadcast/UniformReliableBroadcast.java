package cs451.abstraction.broadcast;

import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UniformReliableBroadcast extends Broadcaster {

    final private static AtomicInteger ZERO = new AtomicInteger(0);

    final private int hostId;
    final private int halfNumberOfHosts;

    final private Set<Payload> delivered;
    final private Set<Payload> pending;
    final private Set<Payload> doneBroadcast;
    final private Set<Payload> toDeliver;
    final private Map<Payload, Set<Integer>> seenBy;
    final private Map<Payload, AtomicInteger> numberOfHostsThatSeenMessage;

    final private MessageFactory messageFactory;
    final private BestEffortBroadcast bestEffortBroadcast;
    final private Thread deliveryThread;

    public UniformReliableBroadcast(int hostId, List<Host> allHosts, PayloadFactory payloadFactory) {
        this.hostId = hostId;
        this.halfNumberOfHosts = allHosts.size() / 2;

        this.delivered = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.pending = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.doneBroadcast = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.toDeliver = Collections.newSetFromMap(new ConcurrentHashMap<>());

        this.seenBy = new ConcurrentHashMap<>();
        this.numberOfHostsThatSeenMessage = new ConcurrentHashMap<>();

        this.messageFactory = new MessageFactory(new HostResolver(allHosts));
        this.bestEffortBroadcast = new BestEffortBroadcast(hostId, allHosts, payloadFactory);
        bestEffortBroadcast.registerDeliveryObserver(this);
        bestEffortBroadcast.registerBroadcastObserver(this);

        this.deliveryThread = startDeliveryThread();
    }

    @Override
    public void broadcast(Payload payload) {
        addToPending(payload);
        bestEffortBroadcast.broadcast(payload, false);
        doneBroadcast.add(payload);
    }

    private void addToPending(Payload payload) {
        pending.add(payload);
        toDeliver.add(payload);
    }

    @Override
    public void notifyOfBroadcast(Payload payload) {
        if (payload.getOriginalSenderId() == hostId) {
            emitBroadcastEvent(payload);
        }
    }

    @Override
    public void notifyOfDelivery(Message message) {
        Payload payload = message.getPayload();

        recordSenderHaveSeenMessage(message);
        if (!isPending(payload)) {
            relay(payload);
        }
    }

    private void recordSenderHaveSeenMessage(Message message) {
        Payload payload = message.getPayload();
        numberOfHostsThatSeenMessage.putIfAbsent(payload, new AtomicInteger(0));
        seenBy.putIfAbsent(payload, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        boolean wasAdded = seenBy.get(payload).add(message.getData().getSenderHostId());
        if (wasAdded) numberOfHostsThatSeenMessage.get(payload).incrementAndGet();
    }

    private void relay(Payload payload) {
        addToPending(payload);
        bestEffortBroadcast.broadcast(payload, true);
    }

    private void runDelivery() {
        while (!Thread.interrupted()) {
            processDeliveries();
        }
    }

    private void processDeliveries() {
        Set<Payload> toRemove = new HashSet<>();

        toDeliver.forEach(payload -> {
            // own origin messages should not be delivered through the relayed broadcast from the network
            boolean isOfOwnOrigin = isOfOwnOrigin(payload);
            boolean isOfOwnOriginAndBroadcast = isOfOwnOrigin && doneBroadcast.contains(payload);

            if (canDeliver(payload) && (!isOfOwnOrigin || isOfOwnOriginAndBroadcast)) {
                deliver(createDeliveredMessageFromPayload(payload));
                toRemove.add(payload);
                if (isOfOwnOriginAndBroadcast) {
                    doneBroadcast.remove(payload);
                }
            }
        });
        toDeliver.removeAll(toRemove);
    }

    private Message createDeliveredMessageFromPayload(Payload payload) {
        return messageFactory.createMessageWithPayload(payload.getOriginalSenderId(), hostId, payload);
    }

    private void deliver(Message message) {
        delivered.add(message.getPayload());
        emitDeliverEvent(message);
    }

    private boolean canDeliver(Payload payload) {
        return isPending(payload) && wasSeenByMoreThanHalfHosts(payload) && isNotDelivered(payload);
    }

    private boolean isPending(Payload payload) {
        return pending.contains(payload);
    }

    private boolean isNotDelivered(Payload payload) {
        return !delivered.contains(payload);
    }

    private boolean wasSeenByMoreThanHalfHosts(Payload payload) {
        return numberOfHostsThatSeenMessage.getOrDefault(payload, ZERO).get() > halfNumberOfHosts;
    }

    private boolean isOfOwnOrigin(Payload payload) {
        return payload.getOriginalSenderId() == hostId;
    }

    private Thread startDeliveryThread() {
        Thread deliveryThread = new Thread(this::runDelivery);
        deliveryThread.start();
        return deliveryThread;
    }

    private void stopDeliveryThread() {
        deliveryThread.interrupt();
    }

    @Override
    public void stop() {
        stopDeliveryThread();
        bestEffortBroadcast.stop();
    }
}

