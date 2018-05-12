package konopka.gerrit.data;

import konopka.gerrit.data.entities.AccountDto;

import java.sql.SQLException;
import java.util.List;


public interface IAccountsRepository
        extends IRepository
{
    AccountDto add(AccountDto account) throws SQLException;

    List<AccountDto> getAll() throws SQLException;
}
