package cs451.abstraction.link;

import cs451.parser.Host;

public interface Link {
    void send(Host receiver, DatagramData message);
    void deliver(Message message);
}
