package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class LocalizedCausalPayload implements Payload {

    final private MessagePassedVectorClock vectorClock;
    final private Payload payload;

    final private int sizeInBytes;

    public LocalizedCausalPayload(MessagePassedVectorClock vectorClock, Payload payload) {
        this.vectorClock = vectorClock;
        this.payload = payload;
        this.sizeInBytes = vectorClock.getSizeInBytes() + payload.getSizeInBytes();
    }

    @Override
    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(sizeInBytes);
        buffer.put(vectorClock.getBytes());
        buffer.put(payload.getBytes());
        return buffer.array();
    }

    @Override
    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public MessagePassedVectorClock getVectorClock() {
        return vectorClock;
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    public static LocalizedCausalPayload unpackLocalizedCausalPayload(Payload payload) {
        if (payload instanceof LocalizedCausalPayload) return (LocalizedCausalPayload) payload;
        else return unpackLocalizedCausalPayload(payload.getPayload());
    }

    public int getOriginalSenderId() {
        return payload.getOriginalSenderId();
    }
}
