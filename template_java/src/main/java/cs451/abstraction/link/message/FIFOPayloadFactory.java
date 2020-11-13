package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class FIFOPayloadFactory implements PayloadFactory {

    final private URBPayloadFactory urbPayloadFactory = new URBPayloadFactory();

    @Override
    public Payload create(ByteBuffer buffer) {
        URBPayload urbPayload = urbPayloadFactory.create(buffer);
        int sequenceNumber = buffer.getInt();
        return new FIFOPayload(urbPayload, sequenceNumber);
    }

    public FIFOPayload create(int senderId, int sequenceNumber) {
        return new FIFOPayload(new URBPayload(senderId), sequenceNumber);
    }
}
