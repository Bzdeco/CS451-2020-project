package cs451.abstraction.broadcast;

import cs451.abstraction.Notifier;
import cs451.abstraction.Observer;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UniformReliableBroadcast extends Notifier implements Broadcaster, Observer {

    final private static AtomicInteger ZERO = new AtomicInteger(0);

    final private int halfN;

    final private Set<Payload> delivered;
    final private Set<Payload> pending;
    final private Map<Payload, AtomicBoolean> canDeliverImmediately;
    final private Map<Payload, Set<Integer>> seenBy;
    final private Map<Payload, AtomicInteger> numberOfHostsThatSeenMessage;

    final private BestEffortBroadcast bestEffortBroadcast;

    public UniformReliableBroadcast(int hostId, List<Host> allHosts, PayloadFactory rawPayloadFactory) {
        this.halfN = allHosts.size() / 2;

        this.delivered = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.pending = Collections.newSetFromMap(new ConcurrentHashMap<>());

        this.seenBy = new ConcurrentHashMap<>();
        this.numberOfHostsThatSeenMessage = new ConcurrentHashMap<>();
        this.canDeliverImmediately = new ConcurrentHashMap<>();

        this.bestEffortBroadcast = new BestEffortBroadcast(hostId, allHosts, new FIFOPayloadFactory(rawPayloadFactory));
        bestEffortBroadcast.registerDeliveryObserver(this);
    }

    @Override
    public void broadcast(Payload payload) {
        pending.add(payload);
        bestEffortBroadcast.broadcast(payload);
    }

    @Override
    public void notifyOfDelivery(Message message) {
        Payload payload = message.getPayload();

        if (canDeliverImmediately(payload)) {
            deliver(message);
        } else {
            recordSenderHaveSeenMessage(message);
            relay(payload);

            if (canDeliver(payload)) {
                canDeliverImmediately.get(payload).set(true);
                deliver(message);
                cleanUpMemoryAfterDelivering(payload);
            }
        }
    }

    private void deliver(Message message) {
        delivered.add(message.getPayload());
        emitDeliverEvent(message);
    }

    private void relay(Payload payload) {
        if (!pending.contains(payload)) {
            pending.add(payload);
            bestEffortBroadcast.broadcast(payload);
        }
    }

    private boolean canDeliverImmediately(Payload payload) {
        return isNotDelivered(payload) && canDeliverImmediately.get(payload).get();
    }

    private void recordSenderHaveSeenMessage(Message message) {
        Payload payload = message.getPayload();
        numberOfHostsThatSeenMessage.putIfAbsent(payload, new AtomicInteger(0));
        seenBy.putIfAbsent(payload, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        boolean wasAdded = seenBy.get(payload).add(message.getData().getSenderHostId());
        if (wasAdded) numberOfHostsThatSeenMessage.get(payload).incrementAndGet();
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
        return numberOfHostsThatSeenMessage.getOrDefault(payload, ZERO).get() > halfN;
    }

    private void cleanUpMemoryAfterDelivering(Payload payload) {
        canDeliverImmediately.get(payload).set(true);
        numberOfHostsThatSeenMessage.remove(payload);
        seenBy.remove(payload);
    }

    @Override
    public void stop() {
        bestEffortBroadcast.stop();
    }
}

