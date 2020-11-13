package cs451.abstraction.link.message;

import java.util.Objects;

public class RawPayload implements Payload {

    final private int data = 0;

    @Override
    public byte[] getBytes() {
        return new byte[0];
    }

    @Override
    public int getSizeInBytes() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawPayload that = (RawPayload) o;
        return data == that.data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
