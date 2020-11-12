package cs451.abstraction;

import cs451.abstraction.link.message.Message;

public interface Observer {

    default void notifyOfDelivery(Message message) {}

    default void notifyOfBroadcast(int sequenceNumber) {}
}
