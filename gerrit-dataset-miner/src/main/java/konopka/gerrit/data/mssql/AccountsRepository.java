package konopka.gerrit.data.mssql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import konopka.gerrit.data.IAccountsRepository;
import konopka.gerrit.data.Repository;
import konopka.gerrit.data.entities.AccountDto;


public class AccountsRepository
        extends Repository
        implements IAccountsRepository
{
    // language=SQL
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS Account (" +
            "Id       INT AUTO_INCREMENT NOT NULL," +
            "Name     VARCHAR(255)               ," +
            "Email    VARCHAR(255)               ," +
            "Username VARCHAR(255)               ," +
            "GerritId INT                        ," +
            "PRIMARY KEY (Id)" +
            ");";

    private static final String SELECT_ACCOUNTS_QUERY = "SELECT Id, Name, Email, Username, GerritId FROM Account";
    private static final String INSERT_ACCOUNT_QUERY = "INSERT INTO Account (Name, Email, Username, GerritId) VALUES " +
            "(?, ?, ?, ?)";

    private final Connection connection;


    AccountsRepository(Connection connection)
    {
        this.connection = connection;
    }


    @Override
    public void init() throws SQLException
    {
        executeSqlStatement(connection, CREATE_TABLE_QUERY);
    }


    @Override
    public AccountDto add(AccountDto account) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_ACCOUNT_QUERY, Statement.RETURN_GENERATED_KEYS))
        {

            if (account.name != null)
            {
                stmt.setString(1, account.name);
            }
            else
            {
                stmt.setNull(1, Types.VARCHAR);
            }

            if (account.email != null)
            {
                stmt.setString(2, account.email);
            }
            else
            {
                stmt.setNull(2, Types.VARCHAR);
            }

            if (account.username != null)
            {
                stmt.setString(3, account.username);
            }
            else
            {
                stmt.setNull(3, Types.VARCHAR);
            }

            if (account.accountId != null)
            {
                stmt.setInt(4, account.accountId);
            }
            else
            {
                stmt.setNull(4, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0)
            {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys())
                {
                    if (generatedKeys.next())
                    {
                        account.id = generatedKeys.getInt(1);
                    }
                }
            }

            return account;

        }

    }

    @Override
    public List<AccountDto> getAll() throws SQLException
    {
        try (Statement stmt = connection.createStatement())
        {
            try (ResultSet results = stmt.executeQuery(SELECT_ACCOUNTS_QUERY))
            {
                List<AccountDto> accounts = new ArrayList<>();

                while (results.next())
                {
                    AccountDto account = new AccountDto(results.getInt("Id"), results.getString("Name"), results
                            .getString("Email"), results.getString("Username"), results.getInt("GerritId"));

                    accounts.add(account);
                }

                return accounts;
            }
        }
    }

}
