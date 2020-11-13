package cs451.abstraction.link.message;

import java.nio.ByteBuffer;
import java.util.Objects;

public class FIFOPayload implements Payload {

    final private static int HEADER_BYTE_SIZE = Integer.BYTES;

    final private int sequenceNumber;
    final private Payload payload;

    public FIFOPayload(int sequenceNumber, Payload payload) {
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Payload getPayload() {
        return payload;
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTE_SIZE + payload.getSizeInBytes());
        buffer.putInt(sequenceNumber);
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
        FIFOPayload that = (FIFOPayload) o;
        return sequenceNumber == that.sequenceNumber &&
                payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceNumber, payload);
    }
}
