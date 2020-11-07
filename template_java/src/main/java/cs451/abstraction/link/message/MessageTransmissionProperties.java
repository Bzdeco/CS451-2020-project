package cs451.abstraction.link.message;

import java.time.Instant;

public class MessageTransmissionProperties {

    private Instant sendTime;
    private int retries;

    public MessageTransmissionProperties() {
        this.sendTime = Instant.now();
        this.retries = -1;
    }

    public Instant getSendTime() {
        return sendTime;
    }

    public int getRetries() {
        return retries;
    }

    public void markSending() {
        this.sendTime = Instant.now();
        this.retries++;
    }
}
