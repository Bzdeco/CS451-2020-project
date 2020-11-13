package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class FIFOPayloadFactory implements PayloadFactory {

    final private URBPayloadFactory urbPayloadFactory;

    public FIFOPayloadFactory(PayloadFactory rawPayloadFactory) {
        this.urbPayloadFactory = new URBPayloadFactory(rawPayloadFactory);
    }

    @Override
    public Payload create(ByteBuffer buffer) {
        int sequenceNumber = buffer.getInt();
        URBPayload urbPayload = urbPayloadFactory.create(buffer);
        return new FIFOPayload(sequenceNumber, urbPayload);
    }

    public FIFOPayload create(int senderId, int sequenceNumber, Payload payload) {
        return new FIFOPayload(sequenceNumber, new URBPayload(senderId, payload));
    }
}
