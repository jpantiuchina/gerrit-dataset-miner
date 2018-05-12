package konopka.gerrit.clients;

import java.sql.SQLException;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.GitPerson;
import com.google.gerrit.extensions.restapi.Url;
import konopka.gerrit.data.IAccountsRepository;
import konopka.gerrit.data.cache.AccountsCache;
import konopka.gerrit.data.entities.AccountDto;
import konopka.util.Logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AccountsClient
{
    private static final Logger logger = LoggerFactory.getLogger(AccountsClient.class);
    private final IAccountsRepository repo;
    private final AccountsCache cache;
    private final WaitCaller caller;
    private GerritApi api;


    public AccountsClient(GerritApi api, WaitCaller caller, IAccountsRepository repo)
    {
        this.api = api;
        this.repo = repo;
        this.caller = caller;
        this.cache = new AccountsCache();
    }


    public void prepare() throws SQLException
    {
        cache.restore(repo.getAll());
    }


    public AccountDto get(String name, String email) throws SQLException
    {
        AccountDto account = cache.tryGetCached(email, name, null);
        if (account != null)
        {
            return account;
        }

        AccountInfo info = getAccountInfo(email);
        if (info == null)
        {
            info = getAccountInfo(Url.encode(name));
        }

        if (info != null && cache.isCached(info.email, info.name, info._accountId))
        {
            return cache.tryGetCached(info.email, info.name, info._accountId);
        }

        account = info == null ? saveGitAccount(name, email) : saveGerritAccount(info);
        if (account != null)
        {
            cache.cache(account);
        }
        return account;
    }


    private AccountInfo getAccountInfo(String id)
    {
        AccountInfo info = null;
        try
        {
            info = caller.waitOrCall(() -> api.accounts().id(id).get());
        }
        catch (Exception e)
        {
            logger.error(Logging.prepareWithPart("getAccountInfo", "Account not found", id));
        }
        return info;
    }


    public AccountDto get(GitPerson person) throws SQLException
    {
        if (person == null)
        {
            throw new IllegalArgumentException("Argument person must not be null.");
        }
        return get(person.name, person.email);
    }


    public AccountDto get(AccountInfo info) throws SQLException
    {
        if (info == null)
        {
            throw new IllegalArgumentException("Argument info must not be null.");
        }

        if (cache.isCached(info.email, info.name, info._accountId))
        {
            return cache.tryGetCached(info.email, info.name, info._accountId);
        }

        AccountDto account = saveGerritAccount(info);
        if (account != null)
        {
            cache.cache(account);
        }
        return account;
    }


    private AccountDto getDefault() throws SQLException
    {

        if (!cache.hasNullAccount())
        {
            cache.setNullAccount(repo.add(AccountDto.CreateNullAccount()));
        }

        return cache.getNullAccount();
    }


    public AccountDto getOrDefault(AccountInfo info) throws SQLException
    {
        return info != null ? get(info) : getDefault();
    }


    public AccountDto getOrDefault(GitPerson person) throws SQLException
    {
        return person != null ? get(person) : getDefault();
    }


    private AccountDto saveGerritAccount(AccountInfo info) throws SQLException
    {
        if (info != null)
        {
            AccountDto account = new AccountDto(info.name, info.email, info.username, info._accountId);
            account = repo.add(account);

            return account;
        }
        return null;
    }


    private AccountDto saveGitAccount(String name, String email) throws SQLException
    {
        if (email != null)
        {
            AccountDto account = new AccountDto(name, email);
            account = repo.add(account);

            return account;
        }
        return null;
    }
}
