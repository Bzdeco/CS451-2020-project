package cs451.abstraction.link;

import java.net.DatagramPacket;

public interface DatagramDataFactory {

    DatagramData from(DatagramPacket udpPacket);
}
