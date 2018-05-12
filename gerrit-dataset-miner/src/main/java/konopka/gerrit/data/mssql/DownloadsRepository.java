package konopka.gerrit.data.mssql;

import konopka.gerrit.data.IDownloadsRepository;
import konopka.gerrit.data.Repository;
import konopka.gerrit.data.entities.ChangeDownloadDto;
import konopka.gerrit.data.entities.DownloadResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DownloadsRepository extends Repository implements IDownloadsRepository
{
    // language=SQL
    private static final String CREATE_TABLE_DOWNLOADS = "CREATE TABLE IF NOT EXISTS Download (" +
            "ChangeId int NOT NULL," +
            "Result int NOT NULL," +
            "LastAttemptAt datetime NOT NULL," +
            "Attempts int NOT NULL," +
            "PRIMARY KEY (ChangeId)" +
    ')';

    private static final String INSERT_DOWNLOAD_QUERY = "INSERT INTO Download " +
            "(ChangeId, Result, LastAttemptAt, Attempts) " +
            "VALUES(?, ?, ?, ?)";
    private static final String UPDATE_DOWNLOAD_QUERY = "UPDATE Download SET " +
            "Result = ? " + ",LastAttemptAt = ? " + ",Attempts = ? " + " WHERE ChangeId = ?";

    private static final String SELECT_DOWNLOADS_QUERY = "SELECT ChangeId, Result, LastAttemptAt, Attempts " + "FROM Download WHERE ChangeId = ?";

    private final Connection connection;


    DownloadsRepository(Connection connection)
    {
        this.connection = connection;
    }

    @Override
    public void init() throws SQLException
    {
        executeSqlStatement(connection, CREATE_TABLE_DOWNLOADS);
    }

    @Override
    public void addDownload(ChangeDownloadDto download) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_DOWNLOAD_QUERY))
        {
            stmt.setInt(1, download.getChangeId());
            stmt.setInt(2, download.getResult().getValue());
            stmt.setTimestamp(3, download.getLastAttempt());
            stmt.setInt(4, download.getAttempts());
            stmt.execute();
        }
    }


    @Override
    public ChangeDownloadDto getDownload(int changeId) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_DOWNLOADS_QUERY))
        {
            stmt.setInt(1, changeId);

            try (ResultSet results = stmt.executeQuery())
            {
                if (results.next())
                {
                    return new ChangeDownloadDto(
                            results.getInt("ChangeId"),
                            DownloadResult.fromInt(results.getInt("Result")),
                            results.getTimestamp("LastAttemptAt"),
                            results.getInt("Attempts")
                    );
                }

                return new ChangeDownloadDto(changeId);
            }
        }
    }

    private boolean checkDownload(int changeId) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_DOWNLOADS_QUERY))
        {
            stmt.setInt(1, changeId);

            try (ResultSet results = stmt.executeQuery())
            {
                return results.next();
            }
        }
    }


    @Override
    public void saveDownload(ChangeDownloadDto download) throws SQLException
    {
        if (download.isDirty())
        {
            boolean exists = checkDownload(download.getChangeId());


            if (exists)
            {
                updateDownload(download);
            } else
            {
                addDownload(download);
            }
        }
    }

    private void updateDownload(ChangeDownloadDto download) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(UPDATE_DOWNLOAD_QUERY))
        {
            stmt.setInt(1, download.getResult().getValue());
            stmt.setTimestamp(2, download.getLastAttempt());
            stmt.setInt(3, download.getAttempts());
            stmt.setInt(4, download.getChangeId());

            stmt.execute();
        }
    }
}
