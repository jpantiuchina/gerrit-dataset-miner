package konopka.util;

import java.util.List;


public class ArgsHelper
{
    public static String getArgument(List<String> args, String arg) throws IndexOutOfBoundsException, IllegalArgumentException
    {
        if (args.contains(arg))
        {
            int index = args.indexOf(arg);
            if (index >= 0 && index < args.size() - 1) // check whether has next
            {
                return args.get(index + 1);
            }
            throw new IndexOutOfBoundsException();
        }
        throw new IllegalArgumentException();
    }

}
