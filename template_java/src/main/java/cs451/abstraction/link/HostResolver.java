package cs451.abstraction.link;

import cs451.abstraction.link.message.DatagramData;
import cs451.parser.Host;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Host resolveReceiverHost(DatagramData data) {
        return hostMapping.get(data.getReceiverHostId());
    }

    public Host resolveSenderHost(DatagramData data) {
        return hostMapping.get(data.getSenderHostId());
    }

    public Host getHostById(int hostId) {
        return hostMapping.get(hostId);
    }
}