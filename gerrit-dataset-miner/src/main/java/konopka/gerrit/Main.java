package konopka.gerrit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import konopka.util.Logging;


public final class Main
{

    static final String ARG_PROPS = "-p";
    private static final String[] ARGUMENT_STRING = {"-p", "symbols.xml", "-l", "log4j.properties"};
    public static final String ARG_LOG4J = "-l";


    public static void main2(String[] args) throws Exception
    {

        Configuration config;

        List<String> arguments = Collections.emptyList();
        if (ARGUMENT_STRING != null && ARGUMENT_STRING.length > 0)
        {
            arguments = Arrays.asList(ARGUMENT_STRING);
        }

        Logging.initLogging(arguments);
        config = Configuration.initConfiguration(arguments);


        GerritMiner miner = new GerritMiner(config);
        miner.init();
        miner.mine();

    }

    public static void main(String[] args)
    {
        try
        {
            main2(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }



}
