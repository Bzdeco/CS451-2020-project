package cs451.abstraction.link.message;

import java.net.DatagramPacket;

public interface DatagramDataFactory {

    DatagramData from(DatagramPacket udpPacket);
}
