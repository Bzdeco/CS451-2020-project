package cs451.abstraction.link.message;

import cs451.abstraction.ProcessVectorClock;

import java.nio.ByteBuffer;

public class MessagePassedVectorClock {

    final private int[] clockArray;
    final private int sizeInBytes;

    public MessagePassedVectorClock(ProcessVectorClock processVectorClock) {
        clockArray = copyClockArray(processVectorClock);
        sizeInBytes = clockArray.length * Integer.BYTES;
    }

    // TODO: should be done on a copy of process vector clock
    private int[] copyClockArray(ProcessVectorClock processVectorClock) {
        int[] clockArray = new int[processVectorClock.getLength()];
        for (int processId = 1; processId <= clockArray.length; processId++) {
            clockArray[processId - 1] = processVectorClock.getEntryForProcess(processId);
        }
        return clockArray;
    }

    private MessagePassedVectorClock(int[] clockArray) {
        this.clockArray = clockArray;
        sizeInBytes = clockArray.length * Integer.BYTES;
    }

    public static MessagePassedVectorClock createFromBytes(int vectorClockSize, ByteBuffer buffer) {
        int[] clockArray = new int[vectorClockSize];
        for (int entry = 0; entry < vectorClockSize; entry++) {
            clockArray[entry] = buffer.getInt();
        }
        return new MessagePassedVectorClock(clockArray);
    }

    public int getEntryForHost(int hostId) {
        return clockArray[hostId - 1];
    }

    public void setEntryForHost(int hostId, int value) {
        clockArray[hostId - 1] = value;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(sizeInBytes);
        for (int entry : clockArray) {
            buffer.putInt(entry);
        }
        return buffer.array();
    }

    public boolean isLessThanOrEqual(ProcessVectorClock other) {
        for (int processId = 1; processId <= clockArray.length; processId++) {
            if (this.getEntryForHost(processId) > other.getEntryForProcess(processId)) {
                return false;
            }
        }
        return true;
    }
}
