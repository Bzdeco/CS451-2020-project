package cs451.abstraction;

import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.Payload;

public interface Observer {

    default void notifyOfDelivery(Message message) {}

    default void notifyOfBroadcast(Payload payload) {}
}
