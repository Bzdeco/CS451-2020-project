package cs451.abstraction.link;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Inspired after reading/browsing:
 * <ul>
 *     <li>
 *         <a href="http://darrylcauldwell.com/how-to-calculate-optimal-tcp-window-size-for-long-distance-wan-link/">Calculating TCP Window Size</a>
 *         <a href="https://stackoverflow.com/questions/43481132/best-tcp-send-buffer-size">Stack Overflow post 1</a>
 *         <a href="https://www.auvik.com/franklyit/blog/tcp-window-size/">TCP window size</a>
 *     </li>
 * </ul>
 */
public class ThroughputMonitor {

    private static final int SEND_WINDOW_DELTA = 50;
    private static final int MEASUREMENT_TIME_MILLIS = 1000;
    private static final int HISTORY_SIZE = 3;

    private boolean wasRecentlyIncreased;
    final private AtomicInteger sendWindowSize;
    final private AtomicInteger sentMessagesCounter;
    final private AtomicInteger receivedMessagesCounter;

    private float recentHistoryAverage;
    final private List<Integer> receivedMessagesCounterHistory;

    public ThroughputMonitor(int sendWindowSize) {
        this.sendWindowSize = new AtomicInteger(sendWindowSize);
        sentMessagesCounter = new AtomicInteger(0);

        recentHistoryAverage = 0;
        receivedMessagesCounter = new AtomicInteger(0);
        receivedMessagesCounterHistory = new LinkedList<>();
        wasRecentlyIncreased = false;
    }

    public void recordReceivedMessage() {
        receivedMessagesCounter.incrementAndGet();
    }

    public int getSendWindowSize() {
        return sendWindowSize.get();
    }

    public void updateSendWindowSize() {
        receivedMessagesCounterHistory.add(receivedMessagesCounter.get());

        if (receivedMessagesCounterHistory.size() == HISTORY_SIZE) {
            float average = computeAverageReceivedCount();

            if (average > recentHistoryAverage || (average < recentHistoryAverage && !wasRecentlyIncreased)) {
                increaseSendWindowSize();
                wasRecentlyIncreased = true;
            } else {
                decreaseSendWindowSize();
                wasRecentlyIncreased = false;
            }

            recentHistoryAverage = average;
            receivedMessagesCounterHistory.clear();
        }
    }

    private float computeAverageReceivedCount() {
        float sum = 0;
        for (float count : receivedMessagesCounterHistory) sum += count;
        return sum / receivedMessagesCounterHistory.size();
    }

    private void increaseSendWindowSize() {
        sendWindowSize.addAndGet(SEND_WINDOW_DELTA);
    }

    private void decreaseSendWindowSize() {
        sendWindowSize.addAndGet(-SEND_WINDOW_DELTA);
    }

    private void reset() {
        sentMessagesCounter.set(0);
        receivedMessagesCounter.set(0);
    }

    public void runMonitoring() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(MEASUREMENT_TIME_MILLIS);
            } catch (InterruptedException exception) {
                return;
            }
            updateSendWindowSize();
            reset();
        }
    }
}
