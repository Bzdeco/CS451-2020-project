package cs451.abstraction.broadcast;

import cs451.abstraction.Notifier;
import cs451.abstraction.Observer;
import cs451.abstraction.link.message.Payload;

public abstract class Broadcaster extends Notifier implements Observer {

    public abstract void broadcast(Payload payload);

    public abstract void stop();
}
