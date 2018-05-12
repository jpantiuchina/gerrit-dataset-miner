package konopka.util;

import java.io.File;
import java.util.Properties;


public class SymbolMap
{

    private final Properties symbolmap;


    public SymbolMap(File file)
    {
        symbolmap = new Properties();
        try
        {
            System.out.println("Reading symbols.xml: " + file.getAbsolutePath());
            //Populate the symbol map from the XML file
            symbolmap.loadFromXML(file.toURI().toURL().openStream());
            printSymbolsFromXML();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }



    private void printSymbolsFromXML()
    {
        System.out.println(symbolmap.size() + " symbols loaded:"); //entries
        System.out.println("-" + symbolmap.toString().replace(",", "\n-").replace("{", "").replace("}", "").replace(" ", ""));
        System.out.println();
    }



    public String lookupSymbol(String symbol, Object... variables)
    {
        //Retrieve the value of the associated key
        String message = symbolmap.getProperty(symbol);
        if (message == null)
        {
            System.err.println("Symbol " + symbol + " not found.");
            return "";
        }

        return String.format(message, variables);
    }
}
