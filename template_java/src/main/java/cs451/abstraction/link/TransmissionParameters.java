package cs451.abstraction.link;

import java.time.Duration;

/**
 * <p>Retransmission parameters computation follows the TCP specification in RFC 6298.</p>
 *
 * <p>Used resources:
 * <ul>
 *     <li><a href="https://www.baeldung.com/java-measure-elapsed-time">Measuring elapsed time</a></li>
 *     <li><a href="https://www.tutorialspoint.com/duration-compareto-method-in-java">Comparing durations</a></li>
 * </ul>
 * </p>
 */
public class TransmissionParameters {

    final private static int K = 4;
    final private static Duration GRANULARITY = Duration.ofMillis(1);
    final private static double ALPHA = 0.125;
    final private static double BETA = 0.25;
    final private static int BACK_OFF_FACTOR = 2;
    final private static Duration BASE_RETRANSMISSION_TIMEOUT = Duration.ofMillis(100);
    final private static Duration MAX_RETRANSMISSION_TIMEOUT = Duration.ofHours(1);

    private Duration sRoundTripTime;
    private Duration roundTripTimeVariance;
    private Duration retransmissionTimeout;

    public TransmissionParameters() {
        this.sRoundTripTime = null;
        this.roundTripTimeVariance = null;
        this.retransmissionTimeout = BASE_RETRANSMISSION_TIMEOUT;
    }

    public synchronized Duration getRetransmissionTimeout(boolean isStale) {
        if (isStale) {
            return retransmissionTimeout;
        } else {
            if (retransmissionTimeout.compareTo(BASE_RETRANSMISSION_TIMEOUT) < 0) return retransmissionTimeout;
            else return BASE_RETRANSMISSION_TIMEOUT;
        }
    }

    public synchronized void increaseRetransmissionTimeout() {
        Duration newRetransmissionTimeout = retransmissionTimeout.multipliedBy(BACK_OFF_FACTOR);
        if (newRetransmissionTimeout.compareTo(MAX_RETRANSMISSION_TIMEOUT) < 0) {
            this.retransmissionTimeout = newRetransmissionTimeout;
        } else {
            this.retransmissionTimeout = MAX_RETRANSMISSION_TIMEOUT;
        }
    }

    public synchronized void updateRetransmissionTimeout(Duration roundTripTimeMeasurement) {
        if (isFirstUpdate()) {
            sRoundTripTime = roundTripTimeMeasurement;
            roundTripTimeVariance = roundTripTimeMeasurement.dividedBy(2);
        } else {
            roundTripTimeVariance = computeRoundTripTimeVariance(roundTripTimeMeasurement);
            sRoundTripTime = computeSRoundTripTime(roundTripTimeMeasurement);
        }
        retransmissionTimeout = computeUpdatedRetransmissionTimeout();
    }

    private synchronized boolean isFirstUpdate() {
        return this.sRoundTripTime == null && this.roundTripTimeVariance == null;
    }

    private synchronized Duration computeRoundTripTimeVariance(Duration roundTripTimeMeasurement) {
        long RTTVarMillis = roundTripTimeVariance.toMillis();
        long sRTTMillis = sRoundTripTime.toMillis();
        long RTTMeasurementMillis = roundTripTimeMeasurement.toMillis();

        long newRTTMillis = Math.round((1 - BETA) * RTTVarMillis + BETA * Math.abs(sRTTMillis - RTTMeasurementMillis));
        return Duration.ofMillis(newRTTMillis);
    }

    private synchronized Duration computeSRoundTripTime(Duration roundTripTimeMeasurement) {
        long sRTTMillis = sRoundTripTime.toMillis();
        long RTTMeasurementMillis = roundTripTimeMeasurement.toMillis();

        long newRTTMillis = Math.round((1 - ALPHA) * sRTTMillis + ALPHA * RTTMeasurementMillis);
        return Duration.ofMillis(newRTTMillis);
    }

    private synchronized Duration computeUpdatedRetransmissionTimeout() {
        return sRoundTripTime.plus(maxOfGranularityAnd(roundTripTimeVariance.multipliedBy(K)));
    }

    private synchronized Duration maxOfGranularityAnd(Duration duration) {
        int comparison = duration.compareTo(GRANULARITY);
        if (comparison < 0) {
            return GRANULARITY;
        } else {
            return duration;
        }
    }
}
