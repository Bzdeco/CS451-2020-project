package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class RawPayloadFactory implements PayloadFactory {

    @Override
    public Payload create(ByteBuffer buffer) {
        return new RawPayload();
    }
}
