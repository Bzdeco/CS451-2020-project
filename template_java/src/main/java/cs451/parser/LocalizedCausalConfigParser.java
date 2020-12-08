package cs451.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LocalizedCausalConfigParser extends FIFOConfigParser {

    private Map<Integer, Set<Integer>> relationships;

    public LocalizedCausalConfigParser() {
        super();
    }

    @Override
    public boolean populate(String value) {
        super.populate(value);

        try {
            relationships = new HashMap<>();
            List<String> fileContent = Files.readAllLines(Paths.get(getPath()));
            fileContent.remove(0); // remove the line with number of messages to broadcast

            int lineNumber = 1;
            IntStream.range(1, fileContent.size() + 1).forEach(
                    processId -> relationships.put(processId, parseDependencies(fileContent.get(lineNumber)))
            );
            return true;
        } catch (IOException exc) {
            return false;
        }
    }

    private Set<Integer> parseDependencies(String line) {
        String[] affectingProcesses = line.stripTrailing().split(" ");
        return Set.of(affectingProcesses).stream().map(Integer::parseInt).collect(Collectors.toSet());
    }

    public Map<Integer, Set<Integer>> getCausalRelationships() {
        return relationships;
    }
}
