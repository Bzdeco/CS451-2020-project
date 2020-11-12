package cs451.abstraction;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;

public class Logger implements Observer {

    @Override
    public void notifyOfBroadcast(int sequenceNumber) {
        System.out.println("b " + sequenceNumber);
    }

    @Override
    public void notifyOfDelivery(Message message) {
        DatagramData data = message.getData();
        System.out.println("d " + data.getSenderHostId() + " " + data.getSequenceNumber());
    }
}
