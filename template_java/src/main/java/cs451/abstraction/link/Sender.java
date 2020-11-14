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
            TransmissionHistory history = new TransmissionHistory();
            doSend(message);
            history.markSending();
            storage.addUnacknowledgedMessage(message, history);
            return true;
        }
        return false;
    }

    public void queueForSending(Message message) {
        storage.queueForSending(message);
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

    public void sendPendingMessages() {
        Set<Message> successfullySent = new HashSet<>();
        storage.getMessagesToSend().forEach(message -> {
            boolean wasSent = send(message);
            if (wasSent) successfullySent.add(message);
        });
        storage.removeFromToSend(successfullySent);
    }

    public void processPendingAcknowledgmentReplies() {
        Set<Message> pendingAckReply = storage.getPendingAcknowledgmentReplies();
        pendingAckReply.forEach(this::doSend);
        storage.removeFromPendingAcknowledgmentReplies(pendingAckReply);
    }

    public void retransmitUnacknowledgedMessages() {
        retransmitRecentUnacknowledgedMessages();
        retransmitStaleMessages();
    }

    private void retransmitRecentUnacknowledgedMessages() {
        Map<Message, TransmissionHistory> unacknowledgedMessages = storage.getUnacknowledgedMessages();

        unacknowledgedMessages.forEach(((message, history) -> resendIfTimedOut(message, history, false)));

        Set<Message> newStaleMessages = new HashSet<>();
        unacknowledgedMessages.forEach((message, history) -> {
            if (isNumberOfRetriesExceeded(history)) {
                newStaleMessages.add(message);
            }
        });

        storage.moveFromRecentToStale(newStaleMessages);
    }

    private void retransmitStaleMessages() {
        storage.getStaleUnacknowledgedMessages().forEach(((message, history) -> resendIfTimedOut(message, history, true)));
    }

    private synchronized void resendIfTimedOut(Message message, TransmissionHistory history, boolean isStaleMessage) {
        Host receiver = message.getReceiver();
        TransmissionParameters transmissionParameters = storage.getTransmissionParametersFor(receiver.getId());

        if (isTimedOut(history, transmissionParameters, isStaleMessage)) {
            doSend(message);
            history.markSending();
            transmissionParameters.increaseRetransmissionTimeout();
        }
    }

    private boolean isTimedOut(TransmissionHistory history, TransmissionParameters receiverTransmissionParameters,
                               boolean isStaleMessage) {
        Duration elapsedTime = Duration.between(history.getSendTime(), Instant.now());
        return elapsedTime.compareTo(receiverTransmissionParameters.getRetransmissionTimeout(isStaleMessage)) > 0;
    }

    private boolean isNumberOfRetriesExceeded(TransmissionHistory history) {
        return history.getRetries() > MAX_RETRIES_IN_WINDOW;
    }
}
