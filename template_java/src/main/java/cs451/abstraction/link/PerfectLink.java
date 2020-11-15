package cs451.abstraction.link;

import cs451.abstraction.Observer;
import cs451.abstraction.Notifier;
import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.MessageFactory;
import cs451.abstraction.link.message.PayloadFactory;
import cs451.parser.Host;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>{@link PerfectLink} together with {@link Sender}, {@link Receiver} and {@link TransmissionParameters} utilizing
 * implement a protocol reminiscent of and based on TCP (RFC 6298).</p>
 *
 * <p>Used resources:
 * <ul>
 *     <li><a href="https://en.wikipedia.org/wiki/Observer_pattern">Observer design pattern</a></li>
 * </ul>
 * </p>
 */
public class PerfectLink extends Notifier implements Observer {

    final private static int SENDING_SLEEP_TIME = 100;

    final private Sender sender;
    final private Receiver receiver;
    final private Set<DatagramData> delivered;

    final private List<Thread> threads;

    public PerfectLink(Host host, List<Host> allHosts, PayloadFactory payloadFactory, MessageFactory messageFactory) {
        MessagesStorage storage = new MessagesStorage(allHosts);

        this.sender = new Sender(storage);
        this.receiver = new Receiver(host, storage, payloadFactory, messageFactory);
        receiver.registerDeliveryObserver(this);
        this.delivered = Collections.newSetFromMap(new ConcurrentHashMap<>());

        this.threads = new LinkedList<>();
    }

    public void send(Message message) {
        boolean wasSent = sender.send(message);

        while (!wasSent) {
            try {
                Thread.sleep(SENDING_SLEEP_TIME);
                wasSent = sender.send(message);
            } catch (InterruptedException exc) {
                return;
            }
        }
    }

    public void queueForSending(Message message) {
        sender.queueForSending(message);
    }

    @Override
    public void notifyOfDelivery(Message message) {
        DatagramData data = message.getData();
        if (!delivered.contains(data)) {
            delivered.add(data);
            emitDeliverEvent(message);
        }
    }

    public void startThreads() {
        Thread sendingThread = new Thread(this::runSendingAndAcknowledging);
        Thread receivingThread = new Thread(this::runReceivingPackets);
        Thread triagingThread = new Thread(this::runTriagingReceivedPackets);
        threads.addAll(List.of(sendingThread, receivingThread, triagingThread));
        threads.forEach(Thread::start);
    }

    public void stopThreads() {
        threads.forEach(Thread::interrupt);
    }

    private void runSendingAndAcknowledging() {
        while (!Thread.interrupted()) {
            sender.sendPendingMessages();
            sender.retransmitUnacknowledgedMessages();
            sender.processPendingAcknowledgmentReplies();
        }
    }

    private void runReceivingPackets() {
        while (!Thread.interrupted()) {
            receiver.receive();
        }
    }

    private void runTriagingReceivedPackets() {
        while (!Thread.interrupted()) {
            receiver.processReceivedPackets();
        }
    }
}
