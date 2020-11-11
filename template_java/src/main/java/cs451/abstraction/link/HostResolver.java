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
        Host resolvedHost = hostMapping.get(data.getReceiverHostId());
        checkResolvedHost(resolvedHost);
        return resolvedHost;
    }

    public Host resolveSenderHost(DatagramData data) {
        Host resolvedHost = hostMapping.get(data.getSenderHostId());
        checkResolvedHost(resolvedHost);
        return resolvedHost;
    }

    private void checkResolvedHost(Host host) {
        if (host == null) {
            throw new RuntimeException("Unresolved sender of the deserialized packet.");
        }
    }
}