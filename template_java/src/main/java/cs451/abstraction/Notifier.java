package cs451.abstraction;

import cs451.abstraction.link.message.Message;

import java.util.LinkedList;
import java.util.List;

public abstract class Notifier {

    final private List<Observer> deliveryObservers;
    final private List<Observer> broadcastObservers;

    public Notifier() {
        this.deliveryObservers = new LinkedList<>();
        this.broadcastObservers = new LinkedList<>();
    }

    public final void registerDeliveryObserver(Observer observer) {
        deliveryObservers.add(observer);
    }

    public final void registerBroadcastObserver(Observer observer) {
        broadcastObservers.add(observer);
    }

    public final void emitDeliverEvent(Message message) {
        deliveryObservers.forEach(observer -> observer.notifyOfDelivery(message));
    }

    // TODO: should be data
    public final void emitBroadcastEvent(int sequenceNumber) {
        broadcastObservers.forEach(observer -> observer.notifyOfBroadcast(sequenceNumber));
    }
}
