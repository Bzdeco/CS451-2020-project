package cs451.abstraction.link.message;

import java.nio.ByteBuffer;
import java.util.Objects;

// see: https://qr.ae/pNUv2m on Comparable use
public class FIFOPayload implements Comparable<FIFOPayload>, Payload {

    final private static int HEADER_BYTE_SIZE = Integer.BYTES;

    final private int sequenceNumber;
    final private Payload payload;

    public FIFOPayload(int sequenceNumber, Payload payload) {
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    @Override
    public int getOriginalSenderId() {
        return payload.getOriginalSenderId();
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

    @Override
    public String toString() {
        return payload.toString() + " " + sequenceNumber;
    }

    @Override
    public int compareTo(FIFOPayload other) {
        return this.sequenceNumber - other.getSequenceNumber();
    }
}
