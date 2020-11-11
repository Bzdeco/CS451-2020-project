package cs451.abstraction.link;

import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.TransmissionHistory;
import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Sender {

    final private static int MAX_RETRIES_IN_WINDOW = 2; // FIXME: arbitrary

    final private MessagesStorage storage;
    final private Set<Message> messagesToSend;
    final private DatagramSocket sendingSocket;

    public Sender(MessagesStorage storage)
    {
        this.storage = storage;
        this.messagesToSend = new HashSet<>();
        this.sendingSocket = createSendingSocket();
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

    public void processPendingAcknowledgmentReplies() {
        storage.getPendingAcknowledgmentReplies().forEach(this::doSend);
        storage.clearPendingAcknowledgmentReplies();
    }

    public void retransmitUnacknowledgedMessages() {
        Map<Message, TransmissionHistory> unacknowledgedMessages = storage.getUnacknowledgedMessages();

        unacknowledgedMessages.forEach(this::resendIfTimedOut);

        Set<Message> newStaleMessages = new HashSet<>();
        unacknowledgedMessages.forEach((message, history) -> {
            if (isNumberOfRetriesExceeded(history)) newStaleMessages.add(message);
        });

        storage.moveFromRecentToStale(newStaleMessages);
    }

    private void resendIfTimedOut(Message message, TransmissionHistory history) {
        Host receiver = message.getReceiver();
        TransmissionParameters transmissionParameters = storage.getTransmissionParametersFor(receiver.getId());

        if (isTimedOut(history, transmissionParameters)) {
            doSend(message);
            transmissionParameters.increaseRetransmissionTimeout();
        }
    }

    private boolean isTimedOut(TransmissionHistory history, TransmissionParameters receiverTransmissionParameters) {
        Duration elapsedTime = Duration.between(history.getSendTime(), Instant.now());
        return elapsedTime.compareTo(receiverTransmissionParameters.getRetransmissionTimeout()) > 0;
    }

    private boolean isNumberOfRetriesExceeded(TransmissionHistory history) {
        return history.getRetries() > MAX_RETRIES_IN_WINDOW;
    }

    public void processStaleMessages() {
        storage.getStaleUnacknowledgedMessages().forEach(this::resendIfTimedOut);
    }
}
