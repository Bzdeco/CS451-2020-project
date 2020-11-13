package cs451.abstraction;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.FIFOPayload;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.Payload;

public class FIFOLogger implements Observer {

    @Override
    public void notifyOfBroadcast(Payload payload) {
        FIFOPayload fifoPayload = (FIFOPayload) payload;
        System.out.println("b " + fifoPayload.getSequenceNumber());
    }

    @Override
    public void notifyOfDelivery(Message message) {
        DatagramData data = message.getData();
        FIFOPayload fifoPayload = (FIFOPayload) data.getPayload();
        System.out.println("d " + data.getSenderHostId() + " " + fifoPayload.getSequenceNumber());
    }
}
