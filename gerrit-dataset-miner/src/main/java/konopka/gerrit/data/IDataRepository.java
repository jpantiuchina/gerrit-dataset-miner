package konopka.gerrit.data;


public interface IDataRepository
        extends IRepository
{
    IProjectsRepository projects();

    IChangesRepository changes();

    IAccountsRepository accounts();

    IDownloadsRepository downloads();
}
