package cs451.abstraction.broadcast;

import cs451.abstraction.ProcessVectorClock;
import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalizedCausalUniformReliableBroadcast extends Broadcaster {

    final private int hostId;
    final private ProcessVectorClock vectorClock;
    final private Set<Integer> hostDependencies;
    private int lastSequenceNumber;

    final private Set<LocalizedCausalPayload> pending;
    final private Set<Message> toDeliver;

    final private MessageFactory messageFactory;
    final private LocalizedCausalPayloadFactory localizedCausalPayloadFactory;
    final private UniformReliableBroadcast uniformReliableBroadcast;

    public LocalizedCausalUniformReliableBroadcast(int hostId, List<Host> allHosts, Set<Integer> hostDependencies,
                                                   PayloadFactory rawDataPayloadFactory) {
        int numberOfProcesses = allHosts.size();
        this.hostId = hostId;
        vectorClock = new ProcessVectorClock(hostId, numberOfProcesses);
        this.hostDependencies = hostDependencies;
        lastSequenceNumber = 0;

        pending = Collections.newSetFromMap(new ConcurrentHashMap<>());
        toDeliver = Collections.newSetFromMap(new ConcurrentHashMap<>());

        messageFactory = new MessageFactory(new HostResolver(allHosts));
        localizedCausalPayloadFactory = new LocalizedCausalPayloadFactory(numberOfProcesses, rawDataPayloadFactory);
        uniformReliableBroadcast = new UniformReliableBroadcast(hostId, allHosts, localizedCausalPayloadFactory);
        uniformReliableBroadcast.registerBroadcastObserver(this);
        uniformReliableBroadcast.registerDeliveryObserver(this);
    }

    @Override
    public void broadcast(Payload rawPayload) {
        synchronized (vectorClock) {
            MessagePassedVectorClock passedVectorClock = new MessagePassedVectorClock(vectorClock, hostDependencies);
            passedVectorClock.setEntryForHost(hostId, lastSequenceNumber);
            lastSequenceNumber++;

            LocalizedCausalPayload payload = localizedCausalPayloadFactory.create(passedVectorClock, rawPayload);
            uniformReliableBroadcast.broadcast(payload);
        }
    }

    @Override
    public void notifyOfBroadcast(Payload payload) {
        emitBroadcastEvent(payload);
    }

    @Override
    public void notifyOfDelivery(Message message) {
        pending.add(LocalizedCausalPayload.unpackLocalizedCausalPayload(message.getPayload()));

        System.out.println("Pending: " + pending.size());
        Set<LocalizedCausalPayload> toRemove = new HashSet<>();
        // FIXME: this needs to be run in a thread !!!
        pending.forEach(payload -> {
            MessagePassedVectorClock receivedVectorClock = payload.getVectorClock();
//            System.out.println(receivedVectorClock + " <= " + vectorClock + "?");
            if (receivedVectorClock.isLessThanOrEqual(vectorClock)) {
//                System.out.println("yes");
                toRemove.add(payload);
                Message pendingMessage = createDeliveredMessageFromPayload(payload);
                synchronized (vectorClock) {
                    deliver(pendingMessage);
                }
            }
        });
        pending.removeAll(toRemove);
    }

    protected void deliver(Message message) {
        vectorClock.incrementForProcess(message.getPayload().getOriginalSenderId());
        emitDeliverEvent(message);
    }

    private Message createDeliveredMessageFromPayload(Payload payload) {
        return messageFactory.createMessageWithPayload(payload.getOriginalSenderId(), hostId, payload);
    }

    @Override
    public void stop() {
        uniformReliableBroadcast.stop();
    }
}
