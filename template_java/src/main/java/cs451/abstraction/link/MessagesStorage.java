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
 * <p>Used resources:
 * <ul>
 *     <li><a href="https://crunchify.com/hashmap-vs-concurrenthashmap-vs-synchronizedmap-how-a-hashmap-can-be-synchronized-in-java/">ConcurrentHashMap use</a></li>
 *     <li><a href="https://www.geeksforgeeks.org/producer-consumer-solution-using-threads-java/">Producer-consumer in Java</a></li>
 *     <li><a href="https://www.baeldung.com/java-synchronized">Baeldung - synchronization</a></li>
 *     <li><a href="https://stackoverflow.com/questions/5490346/synchronized-methods">Synchronized methods 1</a></li>
 *     <li><a href="https://stackoverflow.com/questions/40072018/about-calling-methods-from-a-synchronized-block">Synchronized methods 2</a></li>
 *     <li><a href="https://www.journaldev.com/378/java-util-concurrentmodificationexception#to-avoid-concurrentmodificationexception-in-multi-threaded-environment">Dealing with ConcurrentModificationException</a></li>
 *     <li><a href="https://stackoverflow.com/questions/6992608/why-there-is-no-concurrenthashset-against-concurrenthashmap">Concurrent set</a></li>
 *     <li><a href="https://stackoverflow.com/questions/25704325/return-copies-of-private-data-rather-than-references">Return copies of fields</a></li>
 *     <li><a href="https://www.baeldung.com/java-immutable-set">Immutable set</a></li>
 * </ul>
 * </p>
 */
public class MessagesStorage {

    final private Map<Integer, TransmissionParameters> transmissionParametersForHosts;
    final private Set<Message> toSend;
    final private Map<Message, TransmissionHistory> recentUnacknowledgedMessages;
    final private Map<Message, TransmissionHistory> staleUnacknowledgedMessages;
    final private Map<DatagramData, Instant> receivedData;
    final private Set<Message> pendingAcknowledgmentReplies;
    final private ThroughputMonitor throughputMonitor;

    public MessagesStorage(List<Host> hosts, ThroughputMonitor throughputMonitor) {
        this.transmissionParametersForHosts = initializeTransmissionParameters(hosts);

        this.toSend = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.recentUnacknowledgedMessages = new ConcurrentHashMap<>();
        this.staleUnacknowledgedMessages = new ConcurrentHashMap<>();
        this.receivedData = new ConcurrentHashMap<>();
        // Concurrent set: https://stackoverflow.com/a/6992643
        this.pendingAcknowledgmentReplies = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.throughputMonitor = throughputMonitor;
    }

    public TransmissionParameters getTransmissionParametersFor(int hostId) {
        return transmissionParametersForHosts.get(hostId);
    }

    private Map<Integer, TransmissionParameters> initializeTransmissionParameters(List<Host> hosts) {
        Map<Integer, TransmissionParameters> transmissionParametersForHosts = new ConcurrentHashMap<>();
        hosts.forEach(host -> transmissionParametersForHosts.put(host.getId(), new TransmissionParameters()));
        return transmissionParametersForHosts;
    }

    public Set<Message> getMessagesToSend() {
        return toSend;
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

    public void queueForSending(Message message) {
        toSend.add(message);
    }

    public boolean canSendMessageImmediately() {
        return recentUnacknowledgedMessages.size() < throughputMonitor.getSendWindowSize();
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

    public void removeFromToSend(Set<Message> messages) {
        toSend.removeAll(messages);
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

    private synchronized void updateTransmissionParametersForReceiver(Message originalMessage, TransmissionHistory history,
                                                          Instant receivedTime) {
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
