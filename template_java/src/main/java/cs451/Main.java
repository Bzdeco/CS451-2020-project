package cs451;

import cs451.abstraction.Logger;
import cs451.abstraction.broadcast.BestEffortBroadcast;
import cs451.abstraction.link.HostResolver;
import cs451.parser.Host;
import cs451.parser.Parser;

import java.util.List;

public class Main {

    private static Logger logger = new Logger();
    private static BestEffortBroadcast broadcaster;

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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> handleSignal()));
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out.println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");
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

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(), parser.signalIp(), parser.signalPort());

        broadcaster = new BestEffortBroadcast(parser.myId(), allHosts, new HostResolver(allHosts));
        broadcaster.registerBroadcastObserver(logger);
        broadcaster.registerDeliveryObserver(logger);

        System.out.println("Waiting for all processes to finish initialization");
        coordinator.waitOnBarrier();

	    System.out.println("Broadcasting messages...");
	    broadcaster.broadcast(10);

	    System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
