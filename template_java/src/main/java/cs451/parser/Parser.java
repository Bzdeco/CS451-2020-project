package cs451.parser;

import java.util.List;

public class Parser {

    private String[] args;
    private long pid;
    private IdParser idParser;
    private HostsParser hostsParser;
    private BarrierParser barrierParser;
    private SignalParser signalParser;
    private OutputParser outputParser;
    private ConfigParser configParser;

    public Parser(String[] args) {
        this.args = args;
    }

    public void parse(ConfigParser confParser) {
        pid = ProcessHandle.current().pid();

        idParser = new IdParser();
        hostsParser = new HostsParser();
        barrierParser = new BarrierParser();
        signalParser = new SignalParser();
        outputParser = new OutputParser();
        configParser = null;

        int argsNum = args.length;
        if (argsNum != Constants.ARG_LIMIT_NO_CONFIG && argsNum != Constants.ARG_LIMIT_CONFIG) {
            System.err.println("Error in number of arguments");
            help();
        }

        if (!idParser.populate(args[Constants.ID_KEY], args[Constants.ID_VALUE])) {
            System.err.println("Error in ID argument");
            help();
        }

        if (!hostsParser.populate(args[Constants.HOSTS_KEY], args[Constants.HOSTS_VALUE])) {
            System.err.println("Error in HOSTS argument");
            help();
        }

        if (!hostsParser.inRange(idParser.getId())) {
            help();
        }

        if (!barrierParser.populate(args[Constants.BARRIER_KEY], args[Constants.BARRIER_VALUE])) {
            System.err.println("Error in BARRIER argument");
            help();
        }

        if (!signalParser.populate(args[Constants.SIGNAL_KEY], args[Constants.SIGNAL_VALUE])) {
            System.err.println("Error in SIGNAL argument");
            help();
        }

        if (!outputParser.populate(args[Constants.OUTPUT_KEY], args[Constants.OUTPUT_VALUE])) {
            System.err.println("Error in OUTPUT argument");
            help();
        }

        if (argsNum == Constants.ARG_LIMIT_CONFIG) {
            configParser = confParser;
            if (!configParser.populate(args[Constants.CONFIG_VALUE])) {
                System.err.println("Error in CONFIG argument");
            }
        }
    }

    private void help() {
        System.err.println("Usage: ./run.sh --id ID --hosts.txt HOSTS --barrier NAME:PORT --signal NAME:PORT --output OUTPUT [config]");
        System.exit(1);
    }

    public int myId() {
        return idParser.getId();
    }

    public List<Host> hosts() {
        return hostsParser.getHosts();
    }

    public String barrierIp() {
        return barrierParser.getIp();
    }

    public int barrierPort() {
        return barrierParser.getPort();
    }

    public String signalIp() {
        return signalParser.getIp();
    }

    public int signalPort() {
        return signalParser.getPort();
    }

    public String output() {
        return outputParser.getPath();
    }

    public boolean hasConfig() {
        return configParser != null;
    }

    public String config() {
        return configParser.getPath();
    }

}
