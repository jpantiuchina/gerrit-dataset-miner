package konopka.gerrit;

import konopka.util.ArgsHelper;
import konopka.util.Logging;
import konopka.util.SymbolMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static konopka.gerrit.Main.ARG_PROPS;


public class Configuration
{
    private static final Logger logger;

    static
    {
        logger = LoggerFactory.getLogger(Configuration.class);
    }

    private String databaseConnectionString = "";
    private String gerritEndpoint = "";
    private String changesQuery = "";
    private long downloadPause;
    private int queryStart;
    private int queryLimit;
    private String miningMode = "";
    private int queryStop;
    private int downloadAttemptsLimit;

    public Configuration(String path)
    {
        setDefaults();
        SymbolMap sm = new SymbolMap(new File(path));
        String connection = sm.lookupSymbol("DatabaseConnectionString");
        if (!connection.isEmpty())
        {
            databaseConnectionString = connection;
        }
        String endpoint = sm.lookupSymbol("GerritEndpoint");
        if (!endpoint.isEmpty())
        {
            gerritEndpoint = endpoint;
        }

        String query = sm.lookupSymbol("ChangesQuery");
        if (!query.isEmpty())
        {
            changesQuery = query;
        }

        String pause = sm.lookupSymbol("DownloadPause");
        if (!pause.isEmpty())
        {
            downloadPause = Long.parseLong(pause);
        }

        String start = sm.lookupSymbol("Start");
        if (!start.isEmpty())
        {
            queryStart = Integer.parseInt(start);
        }

        String limit = sm.lookupSymbol("Limit");
        if (!limit.isEmpty())
        {
            queryLimit = Integer.parseInt(limit);
        }

        String mode = sm.lookupSymbol("MiningMode");
        if (!mode.isEmpty())
        {
            miningMode = mode.toLowerCase().trim();
        }

        String stop = sm.lookupSymbol("Stop");
        if (!stop.isEmpty())
        {
            queryStop = Integer.parseInt(stop);
        }

        String attemptsLimit = sm.lookupSymbol("DownloadAttemptsLimit");
        if (!attemptsLimit.isEmpty())
        {
            downloadAttemptsLimit = Integer.parseInt(attemptsLimit);
        }

        log();
    }


    public Configuration()
    {
        setDefaults();
        log();
    }

    static Configuration initConfiguration(List<String> arguments)
    {
        try
        {
            String props_file = ArgsHelper.getArgument(arguments, ARG_PROPS);
            System.out.println("Loading configuration from file: " + props_file + "...");
            return new Configuration(props_file);
        } catch (IndexOutOfBoundsException e)
        {
            System.err.println("Configuration file expected but path not specified.");
        } catch (IllegalArgumentException e)
        {
            System.err.println("Illegal argument.");
        }

        System.out.println("Loading default configuration...");
        return new Configuration();
    }

    public final String getDatabaseConnectionString()
    {
        return databaseConnectionString;
    }

    public final String getGerritEndpoint()
    {
        return gerritEndpoint;
    }

    public final String getChangesQuery()
    {
        return changesQuery;
    }

    public final long getDownloadPause()
    {
        return downloadPause;
    }

    public final String getMiningMode()
    {
        return miningMode;
    }

    public final int getStop()
    {
        return queryStop;
    }


    private void setDefaults()
    {
        databaseConnectionString = "sqlserver://localhost;user=sa;databaseName=gerrithub-gerrit-dataset;integratedSecurity=false;";
        gerritEndpoint = "https://android-review.googlesource.com/";
        changesQuery = "status:reviewed+OR+status:merged+OR+status:open+OR+status:abandoned";
        downloadPause = 5000;
        queryStart = 0;
        queryLimit = 20;
        queryStop = 300000;
        miningMode = "simple";
        downloadAttemptsLimit = 3;
    }


    private void log()
    {
        logger.info(Logging.prepareWithPart("init", "DatabaseConnectionString: " + databaseConnectionString));
        logger.info(Logging.prepareWithPart("init", "GerritEndpoint: " + gerritEndpoint));
        logger.info(Logging.prepareWithPart("init", "ChangesQuery: " + changesQuery));
        logger.info(Logging.prepareWithPart("init", "DownloadPause: " + downloadPause));
        logger.info(Logging.prepareWithPart("init", "Start: " + queryStart));
        logger.info(Logging.prepareWithPart("init", "Limit: " + queryLimit));
        logger.info(Logging.prepareWithPart("init", "Stop: " + queryStop));
        logger.info(Logging.prepareWithPart("init", "MiningMode: " + miningMode));
        logger.info(Logging.prepareWithPart("init", "DownloadAttemptsLimit: " + downloadAttemptsLimit));

    }

    public int getStart()
    {
        return queryStart;
    }

    public int getLimit()
    {
        return queryLimit;
    }

    public final int getDownloadAttemptsLimit()
    {
        return downloadAttemptsLimit;
    }

}
