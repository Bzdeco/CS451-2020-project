package cs451.abstraction.link.message;

import java.nio.ByteBuffer;
import java.util.Objects;

public class FIFOPayload extends URBPayload {

    final private int properSizeInBytes = Integer.BYTES;
    final private int sequenceNumber;

    public FIFOPayload(URBPayload urbPayload, int sequenceNumber) {
        super(urbPayload);
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public byte[] getBytes() {
        byte[] upperLayerBytes = super.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(upperLayerBytes.length + properSizeInBytes);
        buffer.put(upperLayerBytes);
        buffer.putInt(sequenceNumber);
        return buffer.array();
    }

    @Override
    public int getSizeInBytes() {
        return super.getSizeInBytes() + properSizeInBytes;
    }

    @Override
    public Payload decode(ByteBuffer buffer) {
        URBPayload urbPayload = super.concreteDecode(buffer);
        int sequenceNumber = buffer.getInt();
        return new FIFOPayload(urbPayload, sequenceNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FIFOPayload that = (FIFOPayload) o;
        return sequenceNumber == that.sequenceNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sequenceNumber);
    }
}
