package cs451.abstraction.broadcast;

import cs451.abstraction.ProcessVectorClock;
import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class LocalizedCausalUniformReliableBroadcast extends Broadcaster {

    final private int hostId;
    final private ProcessVectorClock vectorClock;
    final private Set<Integer> hostDependencies;
    private int lastSequenceNumber;

    final private Map<Integer, BlockingQueue<LocalizedCausalPayload>> pendingQueues;
    final private Thread deliveryThread;

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

        pendingQueues = initializePendingQueues(allHosts);
        deliveryThread = startDeliveryThread();

        messageFactory = new MessageFactory(new HostResolver(allHosts));
        localizedCausalPayloadFactory = new LocalizedCausalPayloadFactory(numberOfProcesses, rawDataPayloadFactory);
        uniformReliableBroadcast = new UniformReliableBroadcast(hostId, allHosts, localizedCausalPayloadFactory);
        uniformReliableBroadcast.registerBroadcastObserver(this);
        uniformReliableBroadcast.registerDeliveryObserver(this);
    }

    private static Map<Integer, BlockingQueue<LocalizedCausalPayload>> initializePendingQueues(List<Host> allHosts) {
        Map<Integer, BlockingQueue<LocalizedCausalPayload>> pendingQueues = new ConcurrentHashMap<>();
        allHosts.forEach(host -> pendingQueues.put(host.getId(), new PriorityBlockingQueue<>()));
        return pendingQueues;
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
        Payload payload = message.getPayload();
        int senderId = payload.getOriginalSenderId();
        pendingQueues.get(senderId).add((LocalizedCausalPayload) payload);
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
        pendingQueues.forEach((hostId, queue) -> {
            while (canDeliverFrontOfQueue(queue)) {
                synchronized (vectorClock) {
                    try {
                        deliver(queue.take());
                    } catch (InterruptedException exc) {
                        return;
                    }
                }
            }
        });
    }

    private boolean canDeliverFrontOfQueue(Queue<LocalizedCausalPayload> queue) {
        return !queue.isEmpty() && queue.peek().getVectorClock().isLessThanOrEqual(vectorClock);
    }

    protected void deliver(Payload payload) {
        vectorClock.incrementForProcess(payload.getOriginalSenderId());
        emitDeliverEvent(createDeliveredMessageFromPayload(payload));
    }

    private Message createDeliveredMessageFromPayload(Payload payload) {
        return messageFactory.createMessageWithPayload(payload.getOriginalSenderId(), hostId, payload);
    }

    @Override
    public void stop() {
        uniformReliableBroadcast.stop();
        deliveryThread.interrupt();
    }
}
