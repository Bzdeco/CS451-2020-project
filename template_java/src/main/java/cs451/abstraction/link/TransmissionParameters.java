package cs451.abstraction.link;

import java.time.Duration;

/**
 * Measuring time:
 * - https://www.baeldung.com/java-measure-elapsed-time
 *
 * Computed retransmission parameters follow the TCP specification in RFC 6298
 */
public class TransmissionParameters {

    final private static int K = 4;
    final private static Duration GRANULARITY = Duration.ofMillis(1);
    final private static double ALPHA = 0.125;
    final private static double BETA = 0.25;
    final private static int BASE_RETRANSMISSION_TIMEOUT_MILLIS = 2;
    final private static int BACK_OFF_FACTOR = 2;

    private Duration sRoundTripTime;
    private Duration roundTripTimeVariance;
    private Duration retransmissionTimeout;

    public TransmissionParameters() {
        this.sRoundTripTime = null;
        this.roundTripTimeVariance = null;
        this.retransmissionTimeout = Duration.ofMillis(BASE_RETRANSMISSION_TIMEOUT_MILLIS);
    }

    public synchronized Duration getRetransmissionTimeout() {
        return retransmissionTimeout;
    }

    public synchronized void increaseRetransmissionTimeout() {
        this.retransmissionTimeout = retransmissionTimeout.multipliedBy(BACK_OFF_FACTOR);
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

    private boolean isFirstUpdate() {
        return this.sRoundTripTime == null && this.roundTripTimeVariance == null;
    }

    private Duration computeRoundTripTimeVariance(Duration roundTripTimeMeasurement) {
        long RTTVarMillis = roundTripTimeVariance.toMillis();
        long sRTTMillis = sRoundTripTime.toMillis();
        long RTTMeasurementMillis = roundTripTimeMeasurement.toMillis();

        long newRTTMillis = Math.round((1 - BETA) * RTTVarMillis + BETA * Math.abs(sRTTMillis - RTTMeasurementMillis));
        return Duration.ofMillis(newRTTMillis);
    }

    private Duration computeSRoundTripTime(Duration roundTripTimeMeasurement) {
        long sRTTMillis = sRoundTripTime.toMillis();
        long RTTMeasurementMillis = roundTripTimeMeasurement.toMillis();

        long newRTTMillis = Math.round((1 - ALPHA) * sRTTMillis + ALPHA * RTTMeasurementMillis);
        return Duration.ofMillis(newRTTMillis);
    }

    private Duration computeUpdatedRetransmissionTimeout() {
        return sRoundTripTime.plus(maxOfGranularityAnd(roundTripTimeVariance.multipliedBy(K)));
    }

    private Duration maxOfGranularityAnd(Duration duration) {
        int comparison = duration.compareTo(GRANULARITY);
        if (comparison < 0) {
            return GRANULARITY;
        } else {
            return duration;
        }
    }
}
