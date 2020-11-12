package cs451.abstraction.link;

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
public class PerfectLink implements DeliveryObserver, Link {

    final private Sender sender;
    final private Receiver receiver;

    final private Set<DatagramData> delivered;
    final private List<DeliveryObserver> deliveryObservers;


    public PerfectLink(Host host, HostResolver hostResolver, MessagesStorage storage, MessageFactory messageFactory) {
        this.sender = new Sender(storage);
        this.receiver = new Receiver(host, storage, hostResolver, messageFactory);
        receiver.registerDeliveryObserver(this);

        this.delivered = new HashSet<>();
        this.deliveryObservers = new LinkedList<>();
    }

    public void registerDeliveryObserver(DeliveryObserver observer) {
        deliveryObservers.add(observer);
    }

    @Override
    public void send(Message message) {
        boolean wasSent = sender.send(message);

        while (!wasSent) {
            try {
                Thread.sleep(10);
                wasSent = sender.send(message);
            } catch (InterruptedException exc) {
                return;
            }
        }
    }

    @Override
    public void deliver(Message message) {
        deliveryObservers.forEach(observer -> observer.notifyOfDelivery(message));
        DatagramData data = message.getData();
        System.out.println("d " + data.getSenderHostId() + " " + data.getSequenceNumber());
    }

    @Override
    public void notifyOfDelivery(Message message) {
        DatagramData data = message.getData();
        if (!delivered.contains(data)) {
            delivered.add(data);
            deliver(message);
        }
    }

    public void runSendingPackets() {
        while (!Thread.interrupted()) {
            sender.retransmitUnacknowledgedMessages();
            sender.processPendingAcknowledgmentReplies();
        }
    }

    public void runReceivingPackets() {
        while (!Thread.interrupted()) {
            receiver.receive();
        }
    }

    public void runTriagingReceivedPackets() {
        while (!Thread.interrupted()) {
            receiver.processReceivedPackets();
        }
    }
}
