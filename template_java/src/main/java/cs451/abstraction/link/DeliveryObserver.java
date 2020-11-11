package cs451.abstraction.link;

import cs451.abstraction.link.message.Message;

public interface DeliveryObserver {

    void notifyOfDelivery(Message message);
}
