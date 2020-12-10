package cs451.abstraction.link.message;

import cs451.abstraction.ProcessVectorClock;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;

public class MessagePassedVectorClock {

    private int hostId;
    final private int[] clockArray;
    final private int sizeInBytes;

    public MessagePassedVectorClock(ProcessVectorClock processVectorClock, Set<Integer> dependencies) {
        hostId = processVectorClock.getHostId();
        clockArray = filteredCopyOfClockArray(processVectorClock, dependencies);
        sizeInBytes = clockArray.length * Integer.BYTES;
    }

    private int[] filteredCopyOfClockArray(ProcessVectorClock processVectorClock, Set<Integer> hostDependencies) {
        int[] clockArray = new int[processVectorClock.getLength()];
        Arrays.fill(clockArray, 0);
        hostDependencies.forEach(
                affectingHostId -> clockArray[affectingHostId - 1] = processVectorClock.getEntryForProcess(affectingHostId)
        );
        return clockArray;
    }

    private MessagePassedVectorClock(int[] clockArray) {
        hostId = 0; // uninitialized, should be subsequently set
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

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
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
