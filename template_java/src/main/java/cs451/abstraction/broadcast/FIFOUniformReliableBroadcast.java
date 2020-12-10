package cs451.abstraction.broadcast;

import cs451.abstraction.link.HostResolver;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Used resources:
 * <ul>
 *     <li><a href="https://www.baeldung.com/java-concurrent-queues">Baeldung - Java concurrent queues</a></li>
 * </ul>
 */
public class FIFOUniformReliableBroadcast extends Broadcaster {

    final private int hostId;
    private int lastSequenceNumber;
    final private Map<Integer, BlockingQueue<FIFOPayload>> pendingQueues;
    final private Map<Integer, Integer> nextToDeliverForProcesses;

    final private MessageFactory messageFactory;
    final private FIFOPayloadFactory fifoPayloadFactory;
    final private UniformReliableBroadcast uniformReliableBroadcast;

    public FIFOUniformReliableBroadcast(int hostId, List<Host> allHosts, PayloadFactory rawDataPayloadFactory) {
        this.hostId = hostId;
        this.lastSequenceNumber = 0;
        this.pendingQueues = initializePendingQueues(allHosts);
        this.nextToDeliverForProcesses = initializeNextToDeliverMapping(allHosts);

        this.messageFactory = new MessageFactory(new HostResolver(allHosts));
        this.fifoPayloadFactory = new FIFOPayloadFactory(rawDataPayloadFactory);
        this.uniformReliableBroadcast = new UniformReliableBroadcast(hostId, allHosts, fifoPayloadFactory);
        uniformReliableBroadcast.registerBroadcastObserver(this);
        uniformReliableBroadcast.registerDeliveryObserver(this);
    }

    private static Map<Integer, BlockingQueue<FIFOPayload>> initializePendingQueues(List<Host> allHosts) {
        Map<Integer, BlockingQueue<FIFOPayload>> pendingQueues = new ConcurrentHashMap<>();
        allHosts.forEach(host -> pendingQueues.put(host.getId(), new PriorityBlockingQueue<>()));
        return pendingQueues;
    }

    private static Map<Integer, Integer> initializeNextToDeliverMapping(List<Host> allHosts) {
        Map<Integer, Integer> nextToDeliver = new ConcurrentHashMap<>();
        allHosts.forEach(host -> nextToDeliver.put(host.getId(), 1));
        return nextToDeliver;
    }

    @Override
    public void broadcast(Payload rawPayload) {
        lastSequenceNumber++;
        FIFOPayload fifoPayload = fifoPayloadFactory.create(hostId, lastSequenceNumber, rawPayload);
        uniformReliableBroadcast.broadcast(fifoPayload);
    }

    @Override
    public void notifyOfBroadcast(Payload payload) {
        emitBroadcastEvent(payload);
    }

    @Override
    public void notifyOfDelivery(Message message) {
        try {
            addToPending(message);
        } catch (InterruptedException exc) {
            return;
        }

        pendingQueues.forEach((hostId, queue) -> {
            int nextToDeliverSequenceNumber = nextToDeliverForProcesses.get(hostId);
            while (canDeliverFrontOfQueue(queue, nextToDeliverSequenceNumber)) {
                try {
                    FIFOPayload payload = queue.take();
                    emitDeliverEvent(createDeliveredMessageFromPayload(payload));
                    nextToDeliverSequenceNumber++;
                } catch (InterruptedException exc) {
                    return;
                }
            }
            nextToDeliverForProcesses.replace(hostId, nextToDeliverSequenceNumber);
        });
    }

    private void addToPending(Message message) throws InterruptedException {
        int senderHostId = message.getData().getSenderHostId();
        pendingQueues.get(senderHostId).put((FIFOPayload) message.getPayload());
    }

    private boolean canDeliverFrontOfQueue(BlockingQueue<FIFOPayload> queue, int nextToDeliverSequenceNumber) {
        return !queue.isEmpty() && (queue.peek().getSequenceNumber() == nextToDeliverSequenceNumber);
    }

    private Message createDeliveredMessageFromPayload(Payload payload) {
        return messageFactory.createMessageWithPayload(payload.getOriginalSenderId(), hostId, payload);
    }

    @Override
    public void stop() {
        uniformReliableBroadcast.stop();
    }
}
