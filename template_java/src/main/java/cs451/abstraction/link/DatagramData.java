package cs451.abstraction.link;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public abstract class DatagramData {

    final protected int senderHostId;

    public DatagramData(int senderHostId) {
        this.senderHostId = senderHostId;
    }

    byte[] convertToBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(senderHostId);
        return buffer.array();
    }

    /**
     * Reads sender host id from the given UDP packet payload. Sender host id must be stored within the first bytes
     * of any transmitted packet.
     *
     * @param udpPacket UDP packet from which the sender id will be read
     * @return sender process id that sent the given packet
     */
    static int getSenderHostId(DatagramPacket udpPacket) {
        ByteBuffer packetBytes = ByteBuffer.wrap(udpPacket.getData());
        return packetBytes.getInt();
    }
}
