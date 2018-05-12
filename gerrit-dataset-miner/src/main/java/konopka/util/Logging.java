package konopka.util;

import org.apache.log4j.PropertyConfigurator;

import java.util.List;

import static konopka.gerrit.Main.ARG_LOG4J;

public class Logging
{


    public static void initLogging(List<String> arguments)
    {
        try
        {
            String log4j_file = ArgsHelper.getArgument(arguments, ARG_LOG4J);
            System.out.println("Loading properties from file: " + log4j_file + "...");
            PropertyConfigurator.configure(log4j_file);
            return;

        } catch (IndexOutOfBoundsException e)
        {
            System.err.println("Properties file expected but path not specified.");
        } catch (IllegalArgumentException e)
        {
            System.err.println("Illegal argument.");
        }

        System.out.println("Loading default properties...");
        PropertyConfigurator.configure("log4j.properties");
    }



    public static String prepare(String method, String... args)
    {
        StringBuilder sb = new StringBuilder(method);

        sb.append("(");
        for (String arg : args)
        {
            sb.append(arg);
            sb.append(", ");
        }
        if (args.length > 0)
        {
            sb.replace(sb.length() - 2, sb.length(), "");
        }
        sb.append(")");

        return sb.toString();
    }



    public static String prepare(String method)
    {
        return method + "()";
    }



    public static String prepareWithPart(String method, String part, String... args)
    {
        return prepare(method, args) + ":" + part;
    }



    public static String prepareWithPart(String method, String part)
    {
        return prepare(method) + ":" + part;
    }

}
