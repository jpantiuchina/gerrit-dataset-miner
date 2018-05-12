package konopka.gerrit.data;

import java.sql.SQLException;

import konopka.gerrit.data.entities.ChangeDto;


public interface IChangesRepository
        extends IRepository
{
    void addChange(ChangeDto change) throws SQLException;

    boolean containsChange(int id) throws SQLException;
}
