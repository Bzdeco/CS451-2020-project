package cs451.abstraction.link;

import cs451.parser.Host;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: should be instantiated as a singleton class at the beginning for the given process 
public class HostResolver {

    final private Map<Integer, Host> hostMapping;

    public HostResolver(List<Host> hosts) {
        this.hostMapping = createHostMapping(hosts);
    }

    private Map<Integer, Host> createHostMapping(List<Host> hosts) {
        Map<Integer, Host> mapping = new HashMap<>();
        hosts.forEach(host -> mapping.put(host.getId(), host));
        return mapping;
    }

    public Host resolveSenderHost(DatagramPacket udpPacket) {
        int senderHostId = DatagramData.getSenderHostId(udpPacket);
        Host resolvedHost = hostMapping.get(senderHostId);
        if (resolvedHost == null) {
            throw new RuntimeException("Unresolved sender of the deserialized packet.");
        }
        return resolvedHost;
    }
}