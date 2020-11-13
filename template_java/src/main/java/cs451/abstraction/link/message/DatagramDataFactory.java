package cs451.abstraction.link.message;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public class DatagramDataFactory {

    final private PayloadFactory payloadFactory;

    public DatagramDataFactory(PayloadFactory payloadFactory) {
        this.payloadFactory = payloadFactory;
    }

    public DatagramData create(DatagramPacket udpPacket) {
        ByteBuffer buffer = ByteBuffer.wrap(udpPacket.getData());

        int senderHostId = buffer.getInt();
        int receiverHostId = buffer.getInt();
        DatagramDataType type = DatagramDataType.fromEncoding(buffer.get());
        Payload payload = payloadFactory.create(buffer);

        return new DatagramData(senderHostId, receiverHostId, type, payload);
    }
}
