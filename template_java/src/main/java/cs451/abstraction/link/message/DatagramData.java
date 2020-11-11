package cs451.abstraction.link.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Objects;

public class DatagramData {

    final private int senderHostId;
    final private int receiverHostId;
    final private DatagramDataType type;
    final private int sequenceNumber;

    public DatagramData(int senderHostId, int receiverHostId, DatagramDataType type, int sequenceNumber) {
        this.senderHostId = senderHostId;
        this.receiverHostId = receiverHostId;
        this.type = type;
        this.sequenceNumber = sequenceNumber;
    }

    public DatagramData(DatagramPacket udpPacket) {
        ByteBuffer buffer = ByteBuffer.wrap(udpPacket.getData());

        this.senderHostId = buffer.getInt();
        this.receiverHostId = buffer.getInt();
        this.type = DatagramDataType.fromEncoding(buffer.get());
        this.sequenceNumber = buffer.getInt();
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
        buffer.putInt(sequenceNumber);
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

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    protected int getSizeInBytes() {
        return 3 * Integer.BYTES + DatagramDataType.getEncodingSizeInBytes();
    }

    public static DatagramData convertReceivedToAcknowledgment(DatagramData received) {
        int ackSenderId = received.getReceiverHostId();
        int ackReceiverId = received.getSenderHostId();
        return new DatagramData(ackSenderId, ackReceiverId, DatagramDataType.ACK, received.getSequenceNumber());
    }

    public static DatagramData convertAcknowledgmentToOriginal(DatagramData acknowledgment) {
        int originalSenderId = acknowledgment.getReceiverHostId();
        int originalReceiverId = acknowledgment.getSenderHostId();
        return new DatagramData(originalSenderId, originalReceiverId, DatagramDataType.PAYLOAD, acknowledgment.getSequenceNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatagramData that = (DatagramData) o;
        return senderHostId == that.senderHostId &&
                receiverHostId == that.receiverHostId &&
                sequenceNumber == that.sequenceNumber &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderHostId, receiverHostId, type, sequenceNumber);
    }
}
