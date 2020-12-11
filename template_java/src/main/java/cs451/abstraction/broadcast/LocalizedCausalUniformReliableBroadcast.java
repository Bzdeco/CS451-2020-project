package cs451.abstraction.broadcast;

import cs451.abstraction.ProcessVectorClock;
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

    final private Set<Message> pending;
    final private Thread deliveryThread;

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
        deliveryThread = startDeliveryThread();

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
        pending.add(message);
    }

    private Thread startDeliveryThread() {
        Thread deliveryThread = new Thread(this::runDelivery);
        deliveryThread.start();
        return deliveryThread;
    }

    private void runDelivery() {
        while (!Thread.interrupted()) {
            processDeliveries();
        }
    }

    private void processDeliveries() {
        Set<Message> toRemove = new HashSet<>();
        pending.forEach(pendingMessage -> {
            LocalizedCausalPayload payload = LocalizedCausalPayload.unpackLocalizedCausalPayload(pendingMessage.getPayload());
            MessagePassedVectorClock receivedVectorClock = payload.getVectorClock();
            if (receivedVectorClock.isLessThanOrEqual(vectorClock)) {
                toRemove.add(pendingMessage);
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

    @Override
    public void stop() {
        deliveryThread.interrupt();
        uniformReliableBroadcast.stop();
    }
}
