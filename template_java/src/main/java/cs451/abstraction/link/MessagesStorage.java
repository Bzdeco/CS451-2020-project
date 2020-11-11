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
    final private static int SEND_WINDOW_SIZE = 10; // FIXME: arbitrary

    final private Map<Integer, TransmissionParameters> transmissionParametersForHosts;
    final private Map<Message, TransmissionHistory> recentUnacknowledgedMessages;
    final private Map<Message, TransmissionHistory> staleUnacknowledgedMessages;
    final private Map<DatagramData, Instant> receivedData;
    final private Set<Message> pendingAcknowledgmentReplies;

    public MessagesStorage(List<Host> hosts) {
        this.transmissionParametersForHosts = initializeTransmissionParameters(hosts);

        int capacity = (int) Math.ceil(SEND_WINDOW_SIZE / LOAD_FACTOR);
        this.recentUnacknowledgedMessages = Collections.synchronizedMap(new HashMap<>(capacity));
        this.staleUnacknowledgedMessages = Collections.synchronizedMap(new HashMap<>());
        this.receivedData = Collections.synchronizedMap(new HashMap<>());
        this.pendingAcknowledgmentReplies = Collections.synchronizedSet(new HashSet<>());
    }

    public TransmissionParameters getTransmissionParametersFor(int hostId) {
        return transmissionParametersForHosts.get(hostId);
    }

    private Map<Integer, TransmissionParameters> initializeTransmissionParameters(List<Host> hosts) {
        Map<Integer, TransmissionParameters> transmissionParametersForHosts = new HashMap<>();
        hosts.forEach(host -> transmissionParametersForHosts.put(host.getId(), new TransmissionParameters()));
        return Collections.synchronizedMap(transmissionParametersForHosts);
    }

    public Map<Message, TransmissionHistory> getUnacknowledgedMessages() {
        return Map.copyOf(recentUnacknowledgedMessages);
    }

    public Map<Message, TransmissionHistory> getStaleUnacknowledgedMessages() {
        return Map.copyOf(staleUnacknowledgedMessages);
    }

    public Set<Message> getPendingAcknowledgmentReplies() {
        return Set.copyOf(pendingAcknowledgmentReplies);
    }

    public synchronized Set<DatagramData> getReceivedData() {
        return receivedData.keySet();
    }

    public boolean canSendMessageImmediately() {
        return recentUnacknowledgedMessages.size() < SEND_WINDOW_SIZE;
    }

    public void addUnacknowledgedMessage(Message message) {
        recentUnacknowledgedMessages.put(message, new TransmissionHistory());
    }

    public synchronized void addReceivedData(DatagramData data) {
        receivedData.put(data, Instant.now());
    }

    public void addAcknowledgmentToSend(Message ackReply) {
        pendingAcknowledgmentReplies.add(ackReply);
    }

    public void removeFromPendingAcknowledgmentReplies(Set<Message> messages) {
        pendingAcknowledgmentReplies.removeAll(messages);
    }

    public void removeFromReceivedData(Set<DatagramData> data) {
        data.forEach(receivedData::remove);
    }

    public synchronized void acknowledge(Message originalMessage, DatagramData ackData) {
        TransmissionHistory history = removeFromUnacknowledged(originalMessage);
        Instant receivedTime = receivedData.get(ackData);

        // Karn's algorithm for RTT samples (RFC 6298)
        if (wasNotRetransmitted(history)) {
            updateTransmissionParametersForReceiver(originalMessage, history, receivedTime);
        }
    }

    private boolean wasNotRetransmitted(TransmissionHistory history) {
        return history.getRetries() == 0;
    }

    private void updateTransmissionParametersForReceiver(Message originalMessage, TransmissionHistory history, Instant receivedTime) {
        Host receiver = originalMessage.getReceiver();
        Duration roundTripTimeMeasurement = Duration.between(history.getSendTime(), receivedTime);
        TransmissionParameters transmissionParameters = transmissionParametersForHosts.get(receiver.getId());
        transmissionParameters.updateRetransmissionTimeout(roundTripTimeMeasurement);
    }

    private TransmissionHistory removeFromUnacknowledged(Message originalMessage) {
        TransmissionHistory history = recentUnacknowledgedMessages.remove(originalMessage);
        if (history == null) history = staleUnacknowledgedMessages.remove(originalMessage);
        return history;
    }

    public void moveFromRecentToStale(Set<Message> newStaleMessages) {
        newStaleMessages.forEach(message -> {
            TransmissionHistory history = recentUnacknowledgedMessages.remove(message);
            staleUnacknowledgedMessages.put(message, history);
        });
    }
}
