package konopka.gerrit.data.cache;

import java.sql.Timestamp;


public class ChangesCache
{


    private Timestamp lastChangeAt;

    public ChangesCache()
    {

    }

    public Timestamp getLastChangeAt()
    {
        return lastChangeAt;
    }

    public void setLastChangeAt(Timestamp time)
    {
        lastChangeAt = time;
    }

    public void restore()
    {

    }
}
