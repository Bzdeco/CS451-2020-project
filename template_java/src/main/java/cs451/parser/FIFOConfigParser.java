package cs451.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Used resources:
 * <ul>
 *     <li><a href="https://www.techiedelight.com/read-contents-file-java-11/">Read strings from file</a></li>
 * </ul>
 */
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
