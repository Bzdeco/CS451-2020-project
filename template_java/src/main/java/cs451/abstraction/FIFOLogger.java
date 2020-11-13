package cs451.abstraction;

import cs451.abstraction.link.message.*;

public class FIFOLogger implements Observer {

    @Override
    public void notifyOfBroadcast(Payload payload) {
        FIFOPayload fifoPayload = (FIFOPayload) payload;
        System.out.println("b " + fifoPayload.getSequenceNumber());
    }

    @Override
    public void notifyOfDelivery(Message message) {
        Payload payload = message.getPayload();
        FIFOPayload fifoPayload = (FIFOPayload) payload;
        URBPayload urbPayload = (URBPayload) fifoPayload.getPayload();
        System.out.println("d " + urbPayload.getOriginalSenderId() + " " + fifoPayload.getSequenceNumber());
    }
}
