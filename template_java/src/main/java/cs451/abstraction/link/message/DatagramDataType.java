package cs451.abstraction.link.message;

/**
 * <p>Used resources:
 * <ul>
 *     <li><a href="https://stackoverflow.com/questions/5292790/convert-integer-value-to-matching-java-enum">Enum as values</a></li>
 * </ul>
 * </p>
 */
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
