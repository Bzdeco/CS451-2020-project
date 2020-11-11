package cs451.abstraction.link;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.TransmissionHistory;
import cs451.parser.Host;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Returning unmodifiable copies of private fields (Set.copyOf):
 * - https://www.baeldung.com/java-immutable-set
 * - https://stackoverflow.com/questions/25704325/return-copies-of-private-data-rather-than-references
 */
public class MessagesStorage {

    final private static float LOAD_FACTOR = 0.75f;
    final private static int SEND_WINDOW_SIZE = 1000; // FIXME: arbitrary

    final private Map<Integer, TransmissionParameters> transmissionParametersForHosts;
    final private Map<Message, TransmissionHistory> recentUnacknowledgedMessages;
    final private Map<Message, TransmissionHistory> staleUnacknowledgedMessages;
    final private Map<DatagramData, Instant> receivedData;

    final private Set<Message> pendingAcknowledgmentReplies;

    public MessagesStorage(List<Host> hosts) {
        this.transmissionParametersForHosts = initializeTransmissionParameters(hosts);

        int capacity = (int) Math.ceil(SEND_WINDOW_SIZE / LOAD_FACTOR);
        this.recentUnacknowledgedMessages = new HashMap<>(capacity);
        this.staleUnacknowledgedMessages = new HashMap<>();
        this.receivedData = new HashMap<>();
        this.pendingAcknowledgmentReplies = new HashSet<>();
    }

    private Map<Integer, TransmissionParameters> initializeTransmissionParameters(List<Host> hosts) {
        Map<Integer, TransmissionParameters> transmissionParametersForHosts = new HashMap<>();
        hosts.forEach(host -> transmissionParametersForHosts.put(host.getId(), new TransmissionParameters()));
        return transmissionParametersForHosts;
    }

    public Map<Message, TransmissionHistory> getUnacknowledgedMessages() {
        return Map.copyOf(recentUnacknowledgedMessages);
    }

    public Map<Message, TransmissionHistory> getStaleUnacknowledgedMessages() {
        return Map.copyOf(staleUnacknowledgedMessages);
    }

    public TransmissionParameters getTransmissionParametersFor(int hostId) {
        return transmissionParametersForHosts.get(hostId);
    }

    public boolean canSendMessageImmediately() {
        return recentUnacknowledgedMessages.size() < SEND_WINDOW_SIZE;
    }

    public void addUnacknowledgedMessage(Message message) {
        recentUnacknowledgedMessages.put(message, new TransmissionHistory());
    }

    public void addReceivedData(DatagramData data) {
        receivedData.put(data, Instant.now());
    }

    public Set<DatagramData> getReceivedData() {
        return Set.copyOf(receivedData.keySet());
    }

    public void addAcknowledgmentToSend(Message ackReply) {
        pendingAcknowledgmentReplies.add(ackReply);
    }

    public Set<Message> getPendingAcknowledgmentReplies() {
        return Set.copyOf(pendingAcknowledgmentReplies);
    }

    public void clearPendingAcknowledgmentReplies() {
        pendingAcknowledgmentReplies.clear();
    }

    public void acknowledge(Message originalMessage, DatagramData ackData) {
        TransmissionHistory history = removeFromUnacknowledged(originalMessage);
        Instant receivedTime = receivedData.get(ackData);

        // Karn's algorithm for RTT samples (RFC 6298)
        if (wasNotRetransmitted(history)) {
            updateTransmissionParametersForReceiver(originalMessage, history, receivedTime);
        }
    }

    private void updateTransmissionParametersForReceiver(Message originalMessage, TransmissionHistory history, Instant receivedTime) {
        Host receiver = originalMessage.getReceiver();
        Duration roundTripTimeMeasurement = Duration.between(history.getSendTime(), receivedTime);
        TransmissionParameters transmissionParameters = transmissionParametersForHosts.get(receiver.getId());
        transmissionParameters.updateRetransmissionTimeout(roundTripTimeMeasurement);
    }

    private boolean wasNotRetransmitted(TransmissionHistory history) {
        return history.getRetries() == 0;
    }

    private TransmissionHistory removeFromUnacknowledged(Message originalMessage) {
        TransmissionHistory history = recentUnacknowledgedMessages.remove(originalMessage);
        if (history == null) history = staleUnacknowledgedMessages.remove(originalMessage);
        return history;
    }

    public int getNumberOfFreeMessageSlots() {
        return SEND_WINDOW_SIZE - recentUnacknowledgedMessages.size();
    }

    public void moveFromRecentToStale(Set<Message> newStaleMessages) {
        newStaleMessages.forEach(message -> {
            TransmissionHistory history = recentUnacknowledgedMessages.remove(message);
            staleUnacknowledgedMessages.put(message, history);
        });
    }

    public void clearReceivedPackets() {
        receivedData.clear();
    }
}
