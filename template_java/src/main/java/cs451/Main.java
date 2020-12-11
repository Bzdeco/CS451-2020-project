package cs451;

import cs451.abstraction.FileLogger;
import cs451.abstraction.Logger;
import cs451.abstraction.broadcast.Broadcaster;
import cs451.abstraction.broadcast.LocalizedCausalUniformReliableBroadcast;
import cs451.abstraction.link.message.RawPayloadFactory;
import cs451.parser.Host;
import cs451.parser.LocalizedCausalConfigParser;
import cs451.parser.Parser;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * <p>General used resources:
 * <ul>
 *     <li><a href="https://stackoverflow.com/questions/18331350/how-to-force-implementation-of-a-method-in-subclass-without-using-abstract">Forcing method implementation</a></li>
 *     <li><a href="https://www.baeldung.com/java-copy-constructor">Copy constructor</a></li>
 * </ul>
 * </p>
 */
public class Main {

    final private static LocalizedCausalConfigParser configParser = new LocalizedCausalConfigParser();

    private static Logger logger;
    private static Broadcaster broadcaster;

    private static void handleSignal() {
        // immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        broadcaster.stop();

        // write/flush output file if necessary
        System.out.println("Writing output.");
        logger.flush();
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::handleSignal));
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse(configParser);

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        int hostId = parser.myId();
        System.out.println("My id is " + hostId + ".");
        System.out.println("List of hosts.txt is:");
        List<Host> allHosts = parser.hosts();
        for (Host host: allHosts) {
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        String outputPath = parser.output();
        System.out.println("Output: " + outputPath);
        // if config is defined; always check before parser.config()
        checkConfigAvailable(parser);
        System.out.println("Config: " + parser.config());

        Coordinator coordinator = new Coordinator(hostId, parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        int numberOfMessagesToBroadcast = configParser.getNumberOfMessagesToBroadcast();
        Set<Integer> hostDependencies = configParser.getCausalRelationships().get(hostId);
        RawPayloadFactory rawPayloadFactory = new RawPayloadFactory();
        initializeBroadcaster(hostId, allHosts, hostDependencies, rawPayloadFactory, outputPath);

        System.out.println("Waiting for all processes to finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");
        IntStream.range(0, numberOfMessagesToBroadcast).forEach(
                messageNumber -> broadcaster.broadcast(rawPayloadFactory.create(null))
        );

	    System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }

    private static void initializeBroadcaster(int hostId, List<Host> allHosts, Set<Integer> hostDependencies,
                                              RawPayloadFactory rawPayloadFactory, String outputPath) {
        logger = new FileLogger(outputPath);
        broadcaster = new LocalizedCausalUniformReliableBroadcast(hostId, allHosts, hostDependencies, rawPayloadFactory);
        broadcaster.registerBroadcastObserver(logger);
        broadcaster.registerDeliveryObserver(logger);
    }

    private static void checkConfigAvailable(Parser parser) {
        if (!parser.hasConfig()) throw new RuntimeException("Config file missing");
    }
}
