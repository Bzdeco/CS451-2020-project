package cs451.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FIFOConfigParser extends ConfigParser {

    public FIFOConfigParser() {
        super();
    }

    public int getNumberOfMessagesToBroadcast() {
        try {
            String fileContent = Files.readString(Paths.get(getPath()));
            return Integer.parseInt(fileContent);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
