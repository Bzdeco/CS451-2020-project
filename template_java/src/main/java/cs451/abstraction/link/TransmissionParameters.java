package cs451.abstraction.link;

import java.time.Duration;

public class TransmissionParameters {

    private Duration retransmissionTimeout;

    final private static int BASE_RTO_MILLIS = 500;
    final private static int BACK_OFF_FACTOR = 2;

    public TransmissionParameters() {
        this.retransmissionTimeout = Duration.ofMillis(BASE_RTO_MILLIS);
    }

    public Duration getRetransmissionTimeout() {
        return retransmissionTimeout;
    }

    public void extendRetransmissionTimeout() {
        this.retransmissionTimeout = retransmissionTimeout.multipliedBy(BACK_OFF_FACTOR);
    }

}
