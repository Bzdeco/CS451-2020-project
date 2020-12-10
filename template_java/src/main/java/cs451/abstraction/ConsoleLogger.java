package cs451.abstraction;

import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.Payload;

public class ConsoleLogger implements Observer {

    @Override
    public void notifyOfBroadcast(Payload payload) {
        System.out.println(createBroadcastLog(payload));
    }

    protected String createBroadcastLog(Payload payload) {
        return "b " + payload.getSequenceNumber();
    }

    @Override
    public void notifyOfDelivery(Message message) {
        System.out.println(createDeliveryLog(message.getPayload()));
    }

    protected String createDeliveryLog(Payload payload) {
        return "d " + payload.getOriginalSenderId() + " " + payload.getSequenceNumber();
    }
}
