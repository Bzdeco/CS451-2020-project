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
    final private DatagramSocket sendingSocket;

    public Sender(MessagesStorage storage)
    {
        this.storage = storage;
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

    public boolean send(Message message) {
        if (storage.canSendMessageImmediately()) {
            doSend(message);
            storage.addUnacknowledgedMessage(message);
            return true;
        }
        return false;
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

    public void processPendingAcknowledgmentReplies() {
        Set<Message> toSend = storage.getPendingAcknowledgmentReplies();
        toSend.forEach(this::doSend);
        storage.removeFromPendingAcknowledgmentReplies(toSend);
    }

    public void retransmitUnacknowledgedMessages() {
        retransmitRecentUnacknowledgedMessages();
        retransmitStaleMessages();
    }

    private void retransmitRecentUnacknowledgedMessages() {
        Map<Message, TransmissionHistory> unacknowledgedMessages = storage.getUnacknowledgedMessages();

        unacknowledgedMessages.forEach(this::resendIfTimedOut);

        Set<Message> newStaleMessages = new HashSet<>();
        unacknowledgedMessages.forEach((message, history) -> {
            if (isNumberOfRetriesExceeded(history)) newStaleMessages.add(message);
        });

        storage.moveFromRecentToStale(newStaleMessages);
    }

    private void retransmitStaleMessages() {
        storage.getStaleUnacknowledgedMessages().forEach(this::resendIfTimedOut);
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
}
