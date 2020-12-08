package cs451.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Used resources:
 * <ul>
 *     <li><a href="https://www.techiedelight.com/read-contents-file-java-11/">Read strings from file</a></li>
 * </ul>
 */
public class FIFOConfigParser extends ConfigParser {

    private int numberOfMessagesToBroadcast;

    public FIFOConfigParser() {
        super();
    }

    @Override
    public boolean populate(String value) {
        super.populate(value);

        try {
            List<String> fileContent = Files.readAllLines(Paths.get(getPath()));
            numberOfMessagesToBroadcast = Integer.parseInt(fileContent.get(0));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    public int getNumberOfMessagesToBroadcast() {
        return numberOfMessagesToBroadcast;
    }
}
