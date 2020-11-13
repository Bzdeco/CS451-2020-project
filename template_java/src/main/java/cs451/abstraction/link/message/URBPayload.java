package cs451.abstraction.link.message;

import java.nio.ByteBuffer;
import java.util.Objects;

public class URBPayload implements Payload {

    final private int properSizeInBytes = Integer.BYTES;
    final private int originalSenderId;

    public URBPayload(int originalSenderId) {
        this.originalSenderId = originalSenderId;
    }

    protected URBPayload(URBPayload urbPayload) {
        this.originalSenderId = urbPayload.getOriginalSenderId();
    }

    public int getOriginalSenderId() {
        return originalSenderId;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(properSizeInBytes);
        buffer.putInt(originalSenderId);
        return buffer.array();
    }

    @Override
    public int getSizeInBytes() {
        return properSizeInBytes;
    }

    @Override
    public Payload decode(ByteBuffer buffer) {
        return concreteDecode(buffer);
    }

    protected URBPayload concreteDecode(ByteBuffer buffer) {
        int originalSenderId = buffer.getInt();
        return new URBPayload(originalSenderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        URBPayload that = (URBPayload) o;
        return originalSenderId == that.originalSenderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalSenderId);
    }
}
