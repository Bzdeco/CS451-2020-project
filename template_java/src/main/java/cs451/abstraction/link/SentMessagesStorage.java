package cs451.abstraction.link;

import cs451.abstraction.link.message.Message;

import java.util.HashSet;
import java.util.Set;

public class SentMessagesStorage {

    final private static float LOAD_FACTOR = 0.75f;
    final private static int SEND_WINDOW_SIZE = 1000; // FIXME: arbitrary

    final private Set<Message> recentUnacknowledgedMessages; // TODO: need to be modified on receiving messages
    final private Set<Message> staleMessages; // TODO: need to be modified on receiving messages

    public SentMessagesStorage() {
        int capacity = (int) Math.ceil(SEND_WINDOW_SIZE / LOAD_FACTOR);
        this.recentUnacknowledgedMessages = new HashSet<>(capacity);
        this.staleMessages = new HashSet<>();
    }

    public Set<Message> getUnacknowledgedMessages() {
        return recentUnacknowledgedMessages;
    }

    public Set<Message> getStaleMessages() {
        return staleMessages;
    }

    public boolean canSendMessageImmediately() {
        return recentUnacknowledgedMessages.size() < SEND_WINDOW_SIZE;
    }

    public void addUnacknowledgedMessage(Message message) {
        recentUnacknowledgedMessages.add(message);
    }

    public int getNumberOfFreeMessageSlots() {
        return SEND_WINDOW_SIZE - recentUnacknowledgedMessages.size();
    }

    public void moveFromUnacknowledgedToStale(Set<Message> newStaleMessages) {
        recentUnacknowledgedMessages.removeAll(newStaleMessages);
        staleMessages.addAll(newStaleMessages);
    }
}
