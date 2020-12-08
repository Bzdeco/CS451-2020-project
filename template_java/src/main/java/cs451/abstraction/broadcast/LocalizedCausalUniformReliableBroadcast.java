package cs451.abstraction.broadcast;

import cs451.abstraction.Notifier;
import cs451.abstraction.Observer;
import cs451.abstraction.ProcessVectorClock;
import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalizedCausalUniformReliableBroadcast extends Notifier implements Observer {

    final private int hostId;
    final private ProcessVectorClock vectorClock;
    final private Set<LocalizedCausalPayload> pending;
    private int lastSequenceNumber;

    final private MessageFactory messageFactory;
    final private LocalizedCausalPayloadFactory localizedCausalPayloadFactory;
    final private UniformReliableBroadcast uniformReliableBroadcast;

    public LocalizedCausalUniformReliableBroadcast(int hostId, List<Host> allHosts,
                                                   PayloadFactory rawDataPayloadFactory) {
        int numberOfProcesses = allHosts.size();
        this.hostId = hostId;
        vectorClock = new ProcessVectorClock(numberOfProcesses);
        pending = Collections.newSetFromMap(new ConcurrentHashMap<>());
        lastSequenceNumber = 0;

        messageFactory = new MessageFactory(new HostResolver(allHosts));
        localizedCausalPayloadFactory = new LocalizedCausalPayloadFactory(numberOfProcesses, rawDataPayloadFactory);
        uniformReliableBroadcast = new UniformReliableBroadcast(hostId, allHosts, localizedCausalPayloadFactory);
        uniformReliableBroadcast.registerBroadcastObserver(this);
        uniformReliableBroadcast.registerDeliveryObserver(this);
    }

    public void broadcast(Payload rawPayload) {
        synchronized (vectorClock) {
            MessagePassedVectorClock passedVectorClock = new MessagePassedVectorClock(vectorClock);
            passedVectorClock.setEntryForHost(hostId, lastSequenceNumber);
            // TODO: limit passed vector clock to dependencies only
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

        Set<LocalizedCausalPayload> toRemove = new HashSet<>();
        pending.forEach(payload -> {
            MessagePassedVectorClock receivedVectorClock = payload.getVectorClock();
            if (receivedVectorClock.isLessThanOrEqual(vectorClock)) {
                toRemove.add(payload);
                synchronized (vectorClock) {
                    vectorClock.incrementForProcess(payload.getOriginalSenderId());
                    emitDeliverEvent(createDeliveredMessageFromPayload(payload));
                }
            }
        });
        pending.removeAll(toRemove);
    }

    private Message createDeliveredMessageFromPayload(Payload payload) {
        return messageFactory.createMessageWithPayload(payload.getOriginalSenderId(), hostId, payload);
    }

    public void stop() {
        uniformReliableBroadcast.stop();
    }
}
