package cs451.abstraction.link.message;

import java.nio.ByteBuffer;
import java.util.Objects;

public class URBPayload implements Payload {

    final private static int HEADER_BYTE_SIZE = Integer.BYTES;

    final private int originalSenderId;
    final private Payload payload;

    public URBPayload(int originalSenderId, Payload payload) {
        this.originalSenderId = originalSenderId;
        this.payload = payload;
    }

    public int getOriginalSenderId() {
        return originalSenderId;
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTE_SIZE + payload.getSizeInBytes());
        buffer.putInt(originalSenderId);
        buffer.put(payload.getBytes());
        return buffer.array();
    }

    @Override
    public int getSizeInBytes() {
        return HEADER_BYTE_SIZE + payload.getSizeInBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        URBPayload that = (URBPayload) o;
        return originalSenderId == that.originalSenderId &&
                payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalSenderId, payload);
    }

    @Override
    public String toString() {
        return payload.toString() + " " + originalSenderId;
    }
}
