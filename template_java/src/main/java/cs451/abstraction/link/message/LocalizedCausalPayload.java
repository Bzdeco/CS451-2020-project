package cs451.abstraction.link.message;

import java.nio.ByteBuffer;
import java.util.Objects;

public class LocalizedCausalPayload implements Comparable<LocalizedCausalPayload>, Payload {

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

    @Override
    public int getOriginalSenderId() {
        return payload.getOriginalSenderId();
    }

    @Override
    public int getSequenceNumber() {
        return vectorClock.getEntryForHost(vectorClock.getHostId()) + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalizedCausalPayload that = (LocalizedCausalPayload) o;
        return vectorClock.equals(that.vectorClock) &&
                payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vectorClock, payload);
    }

    @Override
    public int compareTo(LocalizedCausalPayload payload) {
        MessagePassedVectorClock vectorClock = this.getVectorClock();
        int hostId = vectorClock.getHostId();
        return vectorClock.getEntryForHost(hostId) - payload.getVectorClock().getEntryForHost(hostId);
    }
}
