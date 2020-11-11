package cs451.abstraction.link.message;

public enum DatagramDataType {
    PAYLOAD((byte) 0),
    ACK((byte) 1);

    private final byte encodingValue;

    DatagramDataType(byte encodingValue) {
        this.encodingValue = encodingValue;
    }

    public static DatagramDataType fromEncoding(byte encoding) {
        if (encoding == (byte) 0) {
            return PAYLOAD;
        } else {
            return ACK;
        }
    }

    public byte getEncoding() {
        return encodingValue;
    }

    public static int getEncodingSizeInBytes() {
        return 1;
    }

    @Override
    public String toString() {
        if (encodingValue == (byte) 0) return "PAYLOAD";
        else return "ACK";
    }
}
