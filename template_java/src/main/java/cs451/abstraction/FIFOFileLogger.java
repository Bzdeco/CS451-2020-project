package cs451.abstraction;

import cs451.abstraction.link.message.Message;
import cs451.abstraction.link.message.Payload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class FIFOFileLogger extends FIFOConsoleLogger {

    final private Path outputPath;
    final private Queue<String> loggedEvents;

    public FIFOFileLogger(String outputFilePath) {
        super();
        outputPath = createOutputFile(outputFilePath);
        loggedEvents = new LinkedBlockingQueue<>();
    }

    private static Path createOutputFile(String outputFilePath) {
        Path outputPath = Paths.get(outputFilePath);
        try {
            Files.deleteIfExists(outputPath);
            return Files.createFile(outputPath);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create output file", exception);
        }
    }

    @Override
    public void notifyOfDelivery(Message message) {
        loggedEvents.add(createDeliveryLog(message.getPayload()));
    }

    @Override
    public void notifyOfBroadcast(Payload payload) {
        loggedEvents.add(createBroadcastLog(payload));
    }

    public void flush() {
        try {
            Files.write(outputPath, loggedEvents);
        } catch (IOException exception) {
            throw new RuntimeException("Error while flushing to output file", exception);
        }
    }
}
