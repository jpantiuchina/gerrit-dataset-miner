package konopka.gerrit.data;

import java.sql.SQLException;

import konopka.gerrit.data.entities.ChangeDownloadDto;


public interface IDownloadsRepository
        extends IRepository
{
    void addDownload(ChangeDownloadDto download) throws SQLException;

    ChangeDownloadDto getDownload(int changeId) throws SQLException;

    void saveDownload(ChangeDownloadDto download) throws SQLException;
}
