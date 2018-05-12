package konopka.gerrit.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public abstract class Repository
{
    protected static void executeSqlStatement(Connection connection, String sql) throws SQLException
    {
        try (Statement stmt = connection.createStatement())
        {
            stmt.executeUpdate(sql);
        }
    }
}
