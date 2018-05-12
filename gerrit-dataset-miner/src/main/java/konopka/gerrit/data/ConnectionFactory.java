package konopka.gerrit.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class ConnectionFactory
{
    public static Connection getMSSQLConnection(String connectionString) throws Exception
    {
        Class.forName("com.mysql.jdbc.Driver");

        return DriverManager.getConnection(connectionString);
    }
}

