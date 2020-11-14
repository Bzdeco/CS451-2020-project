package cs451;

import cs451.abstraction.FIFOLogger;
import cs451.abstraction.broadcast.UniformReliableBroadcast;
import cs451.abstraction.link.message.*;
import cs451.parser.Host;
import cs451.parser.Parser;

import java.util.List;
import java.util.stream.IntStream;

public class Main {

    private static FIFOLogger logger = new FIFOLogger();
    private static UniformReliableBroadcast broadcaster;

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");
        broadcaster.stop();

        //write/flush output file if necessary
        System.out.println("Writing output.");
        // TODO: make an additional global object observing broadcast and delivery and writing output
        // logger.flush()
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::handleSignal));
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

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
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
        }

        Coordinator coordinator = new Coordinator(hostId, parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        RawPayloadFactory rawPayloadFactory = new RawPayloadFactory();
        FIFOPayloadFactory payloadFactory = new FIFOPayloadFactory(rawPayloadFactory);
        broadcaster = new UniformReliableBroadcast(hostId, allHosts, payloadFactory);
        broadcaster.registerBroadcastObserver(logger);
        broadcaster.registerDeliveryObserver(logger);

        System.out.println("Waiting for all processes to finish initialization");
        coordinator.waitOnBarrier();

	    System.out.println("Broadcasting messages...");
	    // TODO number of messages from config
        IntStream.range(1, 151).forEach(messageNumber -> {
            FIFOPayload payload = payloadFactory.create(hostId, messageNumber, rawPayloadFactory.create(null));
            broadcaster.broadcast(payload);
        });

	    System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
