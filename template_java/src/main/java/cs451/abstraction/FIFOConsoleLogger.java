package cs451.abstraction;

import cs451.abstraction.link.message.*;

public class FIFOConsoleLogger implements Observer {

    @Override
    public void notifyOfBroadcast(Payload payload) {
        System.out.println(createBroadcastLog(payload));
    }

    protected String createBroadcastLog(Payload payload) {
        FIFOPayload fifoPayload = (FIFOPayload) payload;
        return "b " + fifoPayload.getSequenceNumber();
    }

    @Override
    public void notifyOfDelivery(Message message) {
        System.out.println(createDeliveryLog(message.getPayload()));
    }

    protected String createDeliveryLog(Payload payload) {
        FIFOPayload fifoPayload = (FIFOPayload) payload;
        URBPayload urbPayload = (URBPayload) fifoPayload.getPayload();
        return "d " + urbPayload.getOriginalSenderId() + " " + fifoPayload.getSequenceNumber();
    }
}
