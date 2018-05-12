package konopka.gerrit.clients;

import java.time.Instant;
import java.util.concurrent.Callable;



public class WaitCaller
{

    private long pause;

    private Instant lastCall;



    public WaitCaller(long pause)
    {
        if (pause < 0)
        {
            throw new IllegalArgumentException();
        }

        lastCall = Instant.now().minusMillis(pause);
        this.pause = pause;
    }



    public <T> T waitOrCall(Callable<T> caller) throws Exception
    {

        Instant now = Instant.now();


        if (lastCall.plusMillis(pause).isAfter(now))
        {
            //  long wait = pause - ( now.toEpochMilli() - lastCall.toEpochMilli() );
            Thread.sleep(pause);
        }
        lastCall = now;
        return caller.call();
    }
}
