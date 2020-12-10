package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class LocalizedCausalPayloadFactory implements PayloadFactory {

    final private int numberOfProcesses;
    final private URBPayloadFactory urbPayloadFactory;

    public LocalizedCausalPayloadFactory(int numberOfProcesses, PayloadFactory rawPayloadFactory) {
        this.numberOfProcesses = numberOfProcesses;
        this.urbPayloadFactory = new URBPayloadFactory(rawPayloadFactory);
    }

    @Override
    public Payload create(ByteBuffer buffer) {
        MessagePassedVectorClock vectorClock = MessagePassedVectorClock.createFromBytes(numberOfProcesses, buffer);
        URBPayload urbPayload = urbPayloadFactory.create(buffer);
        vectorClock.setHostId(urbPayload.getOriginalSenderId());
        return new LocalizedCausalPayload(vectorClock, urbPayload);
    }

    public LocalizedCausalPayload create(MessagePassedVectorClock vectorClock, Payload payload) {
        return new LocalizedCausalPayload(vectorClock, payload);
    }
}
