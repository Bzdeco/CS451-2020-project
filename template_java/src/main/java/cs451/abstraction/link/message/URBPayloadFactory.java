package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class URBPayloadFactory implements PayloadFactory {

    final private PayloadFactory payloadFactory;

    public URBPayloadFactory(PayloadFactory payloadFactory) {
        this.payloadFactory = payloadFactory;
    }

    @Override
    public URBPayload create(ByteBuffer buffer) {
        int originalSenderId = buffer.getInt();
        Payload payload = payloadFactory.create(buffer);
        return new URBPayload(originalSenderId, payload);
    }

    public URBPayload create(int senderId, Payload payload) {
        return new URBPayload(senderId, payload);
    }
}
