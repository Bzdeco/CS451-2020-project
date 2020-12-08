package cs451.abstraction.link.message;

public interface Payload {

    byte[] getBytes();

    int getSizeInBytes();

    Payload getPayload();

    int getOriginalSenderId();
}
