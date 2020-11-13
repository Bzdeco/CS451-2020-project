package cs451.abstraction.link.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Objects;

public class DatagramData {

    final private int senderHostId;
    final private int receiverHostId;
    final private DatagramDataType type;
    final private Payload payload;

    public DatagramData(int senderHostId, int receiverHostId, DatagramDataType type, Payload payload) {
        this.senderHostId = senderHostId;
        this.receiverHostId = receiverHostId;
        this.type = type;
        this.payload = payload;
    }

    public final byte[] convertToBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(getSizeInBytes());
        return fillBufferWithDataBytes(buffer).array();
    }

    /**
     * From
     * <a href="https://stackoverflow.com/questions/1936857/convert-integer-into-byte-array-java">Stackoverflow thread</a>
     * and <a href="https://stackoverflow.com/a/5683621">Stackoverflow post</a>.
     */
    protected ByteBuffer fillBufferWithDataBytes(ByteBuffer buffer) {
        buffer.putInt(senderHostId);
        buffer.putInt(receiverHostId);
        buffer.put(type.getEncoding());
        buffer.put(payload.getBytes());
        return buffer;
    }

    public int getSenderHostId() {
        return senderHostId;
    }

    public int getReceiverHostId() {
        return receiverHostId;
    }

    public DatagramDataType getDataType() {
        return type;
    }

    public Payload getPayload() {
        return payload;
    }

    protected int getSizeInBytes() {
        return 2 * Integer.BYTES + DatagramDataType.getEncodingSizeInBytes() + payload.getSizeInBytes();
    }

    public static DatagramData convertReceivedToAcknowledgment(DatagramData received) {
        int ackSenderId = received.getReceiverHostId();
        int ackReceiverId = received.getSenderHostId();
        return new DatagramData(ackSenderId, ackReceiverId, DatagramDataType.ACK, received.getPayload());
    }

    public static DatagramData convertAcknowledgmentToOriginal(DatagramData acknowledgment) {
        int originalSenderId = acknowledgment.getReceiverHostId();
        int originalReceiverId = acknowledgment.getSenderHostId();
        return new DatagramData(originalSenderId, originalReceiverId, DatagramDataType.PAYLOAD, acknowledgment.getPayload());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatagramData that = (DatagramData) o;
        return senderHostId == that.senderHostId &&
                receiverHostId == that.receiverHostId &&
                type == that.type &&
                payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderHostId, receiverHostId, type, payload);
    }

    @Override
    public String toString() {
        return type + " " + senderHostId + " " + receiverHostId + " " + payload;
    }
}
