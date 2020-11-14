package cs451.abstraction;

import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.Payload;

import java.util.LinkedList;
import java.util.List;

public abstract class Notifier {

    final private List<Observer> deliveryObservers;
    final private List<Observer> broadcastObservers;

    public Notifier() {
        this.deliveryObservers = new LinkedList<>();
        this.broadcastObservers = new LinkedList<>();
    }

    public void registerDeliveryObserver(Observer observer) {
        deliveryObservers.add(observer);
    }

    public void registerBroadcastObserver(Observer observer) {
        broadcastObservers.add(observer);
    }

    public final void emitDeliverEvent(Message message) {
        deliveryObservers.forEach(observer -> observer.notifyOfDelivery(message));
    }

    public final void emitBroadcastEvent(Payload payload) {
        broadcastObservers.forEach(observer -> observer.notifyOfBroadcast(payload));
    }
}
