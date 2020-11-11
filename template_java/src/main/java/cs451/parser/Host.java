package cs451.parser;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class Host {

    private static final String IP_START_REGEX = "/";

    private int id;
    private String ip;
    private int port = -1;

    public Host() {}

    public Host(int id, String ip, int port) {
        this.id = id;
        this.ip = formatHostIpAddress(ip).orElse("");
        this.port = port;
    }

    public boolean populate(String idString, String ipString, String portString) {
        try {
            id = Integer.parseInt(idString);
            ip = formatHostIpAddress(ipString).orElse("");
            port = Integer.parseInt(portString);
            if (port <= 0) {
                System.err.println("Port in the hosts.txt file must be a positive number!");
                return false;
            }
        } catch (NumberFormatException e) {
            if (port == -1) {
                System.err.println("Id in the hosts.txt file must be a number!");
            } else {
                System.err.println("Port in the hosts.txt file must be a number!");
            }
            return false;
        }

        return true;
    }

    public static Optional<String> formatHostIpAddress(InetAddress address) {
        String ipString = address.toString();
        if (ipString.startsWith(IP_START_REGEX)) {
            return Optional.of(ipString.substring(1));
        } else {
            String usedAddress = ipString.split(IP_START_REGEX)[0];
            try {
                return Optional.of(InetAddress.getByName(usedAddress).getHostAddress());
            } catch (UnknownHostException exc) {
                System.err.println("Failed to resolve host address " + usedAddress);
                exc.printStackTrace();
                return Optional.empty();
            }
        }
    }

    public static Optional<String> formatHostIpAddress(String textualAddress) {
        try {
            return formatHostIpAddress(InetAddress.getByName(textualAddress));
        } catch (UnknownHostException exc) {
            System.err.println("Failed to resolve host address " + textualAddress);
            exc.printStackTrace();
            return Optional.empty();
        }
    }

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

}
