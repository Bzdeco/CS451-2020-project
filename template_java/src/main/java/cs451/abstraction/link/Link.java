package cs451.abstraction.link;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;
import cs451.parser.Host;

public interface Link {
    void send(Message message);
    void deliver(Message message);
}
