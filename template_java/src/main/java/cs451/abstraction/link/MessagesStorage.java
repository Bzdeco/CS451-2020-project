package cs451.abstraction.link;

import cs451.abstraction.link.message.DatagramData;
import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.TransmissionHistory;
import cs451.parser.Host;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Usage of concurrent collections:
 * -
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
        this.recentUnacknowledgedMessages = new ConcurrentHashMap<>(capacity);
        this.staleUnacknowledgedMessages = new ConcurrentHashMap<>();
        this.receivedData = new ConcurrentHashMap<>();
        // Concurrent set: https://stackoverflow.com/a/6992643
        this.pendingAcknowledgmentReplies = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
        return recentUnacknowledgedMessages;
    }

    public Map<Message, TransmissionHistory> getStaleUnacknowledgedMessages() {
        return staleUnacknowledgedMessages;
    }

    public Set<Message> getPendingAcknowledgmentReplies() {
        return pendingAcknowledgmentReplies;
    }

    public Set<DatagramData> getReceivedData() {
        return receivedData.keySet();
    }

    public boolean canSendMessageImmediately() {
        return recentUnacknowledgedMessages.size() < SEND_WINDOW_SIZE;
    }

    public void addUnacknowledgedMessage(Message message, TransmissionHistory history) {
        recentUnacknowledgedMessages.put(message, history);
    }

    public void addReceivedData(DatagramData data) {
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

    public boolean acknowledge(Message originalMessage, DatagramData ackData) {
        Optional<TransmissionHistory> optHistory = removeFromUnacknowledged(originalMessage);

        optHistory.ifPresent(history -> {
            Instant receivedTime = receivedData.get(ackData);

            // Karn's algorithm for RTT samples (RFC 6298)
            if (wasNotRetransmitted(history)) {
                updateTransmissionParametersForReceiver(originalMessage, history, receivedTime);
            }
        });

        return optHistory.isPresent();
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

    private Optional<TransmissionHistory> removeFromUnacknowledged(Message originalMessage) {
        TransmissionHistory history = recentUnacknowledgedMessages.remove(originalMessage);
        if (history == null) history = staleUnacknowledgedMessages.remove(originalMessage);
        return Optional.ofNullable(history);
    }

    public void moveFromRecentToStale(Set<Message> newStaleMessages) {
        newStaleMessages.forEach(message -> {
            TransmissionHistory history = recentUnacknowledgedMessages.remove(message);
            if (history != null) {
                staleUnacknowledgedMessages.put(message, history);
            }
        });
    }
}
