package cs451.abstraction.link.message;

import java.nio.ByteBuffer;

public class FIFODatagramData extends DatagramData {

    int sequenceNumber;

    public FIFODatagramData(int senderHostId, int sequenceNumber) {
        super(senderHostId);
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * From
     * <a href="https://stackoverflow.com/questions/1936857/convert-integer-into-byte-array-java">Stackoverflow thread</a>
     * and <a href="https://stackoverflow.com/a/5683621">Stackoverflow post</a>.
     *
     * @return datagram data as bytes
     */
    @Override
    byte[] convertToBytes() {
        byte[] initialBytes = super.convertToBytes();

        ByteBuffer buffer = ByteBuffer.allocate(initialBytes.length + Integer.BYTES);
        buffer.put(initialBytes);
        buffer.putInt(sequenceNumber);

        return buffer.array();
    }
}
