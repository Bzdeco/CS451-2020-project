package cs451.abstraction.link.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public class FIFODatagramDataFactory implements DatagramDataFactory {

    /**
     * From
     * <a href="http://bethecoder.com/applications/articles/java/basics/how-to-convert-byte-array-to-integer.html">Be
     * the coder article</a>
     * @param udpPacket a UDP packet to decode as data
     * @return datagram data constructed from received bytes
     */
    @Override
    public DatagramData from(DatagramPacket udpPacket) {
        ByteBuffer buffer = ByteBuffer.wrap(udpPacket.getData());
        int senderHostId = buffer.getInt();
        int sequenceNumber = buffer.getInt();
        return new FIFODatagramData(senderHostId, sequenceNumber);
    }
}
