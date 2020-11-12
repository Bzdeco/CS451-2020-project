package cs451.abstraction.link;

import cs451.abstraction.Observer;
import cs451.abstraction.Notifier;
import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.MessageFactory;
import cs451.parser.Host;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Observer design pattern for registering deliveries: https://en.wikipedia.org/wiki/Observer_pattern
 */
public class PerfectLink extends Notifier implements Observer {

    private static final int SENDING_SLEEP_MILLIS = 10;

    final private Sender sender;
    final private Receiver receiver;
    final private Set<DatagramData> delivered;

    final private List<Thread> threads;

    public PerfectLink(Host host, List<Host> allHosts, MessageFactory messageFactory) {
        MessagesStorage storage = new MessagesStorage(allHosts);

        this.sender = new Sender(storage);
        this.receiver = new Receiver(host, storage, messageFactory);
        receiver.registerDeliveryObserver(this);
        this.delivered = new HashSet<>();

        this.threads = new LinkedList<>();
    }

    public void send(Message message) {
        boolean wasSent = sender.send(message);

        while (!wasSent) {
            try {
                Thread.sleep(SENDING_SLEEP_MILLIS);
                wasSent = sender.send(message);
            } catch (InterruptedException exc) {
                return;
            }
        }
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
        Thread sendingThread = new Thread(this::runRetransmittingAndAcknowledging);
        Thread receivingThread = new Thread(this::runReceivingPackets);
        Thread triagingThread = new Thread(this::runTriagingReceivedPackets);
        threads.addAll(List.of(sendingThread, receivingThread, triagingThread));
        threads.forEach(Thread::start);
    }

    public void stopThreads() {
        threads.forEach(Thread::interrupt);
    }

    private void runRetransmittingAndAcknowledging() {
        while (!Thread.interrupted()) {
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
