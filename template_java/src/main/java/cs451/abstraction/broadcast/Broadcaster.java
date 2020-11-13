package cs451.abstraction.broadcast;

import cs451.abstraction.link.message.Payload;

public interface Broadcaster {

    void broadcast(Payload payload);

    void stop();
}
