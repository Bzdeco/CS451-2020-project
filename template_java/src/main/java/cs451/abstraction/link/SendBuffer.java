package cs451.abstraction.link;

import cs451.parser.Host;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class SendBuffer {

    private Map<Host, TransmissionParameters> transmissionParametersForHosts;
    private Set<Message> recentUnacknowledgedMessages; // TODO: need to be modified on receiving messages
    private Set<Message> staleMessages;
    private Set<Message> messagesToSend;

    private DatagramSocket sendingSocket;

    final private int sendWindowSize;
    final private int maxRetriesInWindow;

    final private static float LOAD_FACTOR = 0.75f;

    public SendBuffer(int sendWindowSize, int maxRetriesInWindow)
    {
        int capacity = (int) Math.floor(sendWindowSize / LOAD_FACTOR);
        this.recentUnacknowledgedMessages = new HashSet<>(capacity);
        this.staleMessages = new HashSet<>();
        this.messagesToSend = new HashSet<>();

        this.sendingSocket = createSendingSocket();

        this.sendWindowSize = sendWindowSize;
        this.maxRetriesInWindow = maxRetriesInWindow;
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
        if (recentUnacknowledgedMessages.size() < sendWindowSize) doSend(message);
        else queueForSending(message);
    }

    private void doSend(Message message) {
        try {
            sendingSocket.send(message.toSentPacket());
            message.markSending();
            recentUnacknowledgedMessages.add(message);
        } catch (IOException exc) {
            System.err.println("Unable to send UDP packet due to I/O exception");
            exc.printStackTrace();
            throw new RuntimeException(exc);
        }
    }

    private void queueForSending(Message message) {
        messagesToSend.add(message);
    }

    public void processPendingMessages() {
        int numberOfUnacknowledgedMessages = recentUnacknowledgedMessages.size();
        int numberOfFreeMessageSlots = sendWindowSize - numberOfUnacknowledgedMessages;

        Iterator<Message> iterator = messagesToSend.iterator();
        while (numberOfFreeMessageSlots > 0 && messagesToSend.size() > 0) {
            Message message = iterator.next();
            doSend(message);
            iterator.remove();
            numberOfFreeMessageSlots--;
        }
    }

    public void retransmitUnacknowledgedMessages() {
        recentUnacknowledgedMessages.forEach(message -> {
            Host receiver = message.getReceiver();
            TransmissionParameters transmissionParameters = transmissionParametersForHosts.get(receiver);
            Duration elapsedTime = Duration.between(message.getTransmissionProperties().getSendTime(), Instant.now());

            if (elapsedTime.compareTo(transmissionParameters.getRetransmissionTimeout()) > 0) {
                doSend(message);
                transmissionParameters.extendRetransmissionTimeout();
            }
        });
    }
}
