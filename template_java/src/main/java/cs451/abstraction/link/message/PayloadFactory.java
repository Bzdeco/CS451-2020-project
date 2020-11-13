package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public interface PayloadFactory {

    Payload create(ByteBuffer buffer);
}
