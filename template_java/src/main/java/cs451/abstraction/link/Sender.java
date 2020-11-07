package cs451.abstraction.link;

import cs451.abstraction.link.message.Message;
import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Sender {

    final private static int MAX_RETRIES_IN_WINDOW = 2; // FIXME: arbitrary

    final private Map<Host, TransmissionParameters> transmissionParametersForHosts;
    final private SentMessagesStorage storage;
    final private Set<Message> messagesToSend;
    final private DatagramSocket sendingSocket;

    public Sender(List<Host> hosts, SentMessagesStorage storage)
    {
        this.transmissionParametersForHosts = initializeTransmissionParameters(hosts);
        this.storage = storage;
        this.messagesToSend = new HashSet<>();
        this.sendingSocket = createSendingSocket();
    }

    private Map<Host, TransmissionParameters> initializeTransmissionParameters(List<Host> hosts) {
        Map<Host, TransmissionParameters> transmissionParametersForHosts = new HashMap<>();
        hosts.forEach(host -> transmissionParametersForHosts.put(host, new TransmissionParameters()));
        return transmissionParametersForHosts;
    }

    private DatagramSocket createSendingSocket() {
        try {
            return new DatagramSocket();
        } catch (SocketException exc) {
            System.err.println("Unable to create sending UDP socket");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    public void send(Message message) {
        if (storage.canSendMessageImmediately()) {
            doSend(message);
            storage.addUnacknowledgedMessage(message);
        } else queueForSending(message);
    }

    private void doSend(Message message) {
        try {
            sendingSocket.send(message.toSentPacket());
            message.markSending();
        } catch (IOException exc) {
            System.err.println("Unable to send a UDP packet due to I/O exception");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    private void queueForSending(Message message) {
        messagesToSend.add(message);
    }

    public void processPendingMessages() {
        int numberOfFreeMessageSlots = storage.getNumberOfFreeMessageSlots();

        Iterator<Message> iterator = messagesToSend.iterator();
        while (numberOfFreeMessageSlots > 0 && messagesToSend.size() > 0) {
            Message message = iterator.next();
            doSend(message);
            storage.addUnacknowledgedMessage(message);
            iterator.remove();
            numberOfFreeMessageSlots--;
        }
    }

    public void retransmitUnacknowledgedMessages() {
        Set<Message> unacknowledgedMessages = storage.getUnacknowledgedMessages();
        unacknowledgedMessages.forEach(this::resendIfTimedOut);
        Set<Message> newStaleMessages = unacknowledgedMessages.stream()
                .filter(this::isNumberOfRetriesExceeded)
                .collect(Collectors.toSet());

        storage.moveFromUnacknowledgedToStale(newStaleMessages);
    }

    private void resendIfTimedOut(Message message) {
        TransmissionParameters transmissionParameters = getTransmissionParameters(message);

        if (isTimedOut(message, transmissionParameters)) {
            doSend(message);
            transmissionParameters.extendRetransmissionTimeout();
        }
    }

    private TransmissionParameters getTransmissionParameters(Message message) {
        return transmissionParametersForHosts.get(message.getReceiver());
    }

    private boolean isTimedOut(Message message, TransmissionParameters receiverTransmissionParameters) {
        Duration elapsedTime = Duration.between(message.getTransmissionProperties().getSendTime(), Instant.now());
        return elapsedTime.compareTo(receiverTransmissionParameters.getRetransmissionTimeout()) > 0;
    }

    private boolean isNumberOfRetriesExceeded(Message message) {
        return message.getTransmissionProperties().getRetries() > MAX_RETRIES_IN_WINDOW;
    }

    public void processStaleMessages() {
        storage.getStaleMessages().forEach(this::resendIfTimedOut);
    }
}
