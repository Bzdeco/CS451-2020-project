package cs451.abstraction.link;

import java.time.Duration;
import java.time.Instant;

public class TransmissionParameters {

    private boolean wasRetransmitted;
    private Duration retransmissionTimeout;
    private Instant lastSentTime; // https://www.baeldung.com/java-measure-elapsed-time

    final private static int BASE_RTO_MILLIS = 500;
    final private static int BACK_OFF_FACTOR = 2;

    public TransmissionParameters() {
        this.wasRetransmitted = false;
        this.retransmissionTimeout = Duration.ofMillis(BASE_RTO_MILLIS);
        this.lastSentTime = null;
    }

    public Instant getLastSentTime() {
        return lastSentTime;
    }

    public Duration getRetransmissionTimeout() {
        return retransmissionTimeout;
    }

    public void extendRetransmissionTimeout() {
        this.retransmissionTimeout = retransmissionTimeout.multipliedBy(BACK_OFF_FACTOR);
    }

}
