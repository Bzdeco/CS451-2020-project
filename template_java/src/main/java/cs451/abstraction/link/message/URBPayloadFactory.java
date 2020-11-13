package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class URBPayloadFactory implements PayloadFactory {

    @Override
    public URBPayload create(ByteBuffer buffer) {
        int originalSenderId = buffer.getInt();
        return new URBPayload(originalSenderId);
    }
}
