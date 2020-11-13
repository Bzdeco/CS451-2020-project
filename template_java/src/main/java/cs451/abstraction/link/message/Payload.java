package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public interface Payload {

    byte[] getBytes();

    int getSizeInBytes();

    Payload decode(ByteBuffer buffer);
}
