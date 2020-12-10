package cs451.abstraction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class ProcessVectorClock {

    final private int hostId;
    final private Map<Integer, AtomicInteger> vectorClock;

    public ProcessVectorClock(int hostId, int numberOfProcesses) {
        this.hostId = hostId;
        vectorClock = initializeVectorClock(numberOfProcesses);
    }

    private Map<Integer, AtomicInteger> initializeVectorClock(int size) {
        Map<Integer, AtomicInteger> vectorClock = new ConcurrentHashMap<>(size);
        IntStream.range(1, size + 1).forEach(processId -> vectorClock.put(processId, new AtomicInteger(0)));
        return vectorClock;
    }

    public int getLength() {
        return vectorClock.size();
    }

    public int getHostId() {
        return hostId;
    }

    public int getEntryForProcess(int processId) {
        return vectorClock.get(processId).get();
    }

    public void incrementForProcess(int processId) {
        vectorClock.get(processId).incrementAndGet();
    }
}
