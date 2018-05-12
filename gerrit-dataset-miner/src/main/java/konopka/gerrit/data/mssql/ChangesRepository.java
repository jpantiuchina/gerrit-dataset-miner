package konopka.gerrit.data.mssql;

import konopka.gerrit.data.IChangesRepository;
import konopka.gerrit.data.Repository;
import konopka.gerrit.data.entities.ChangeApprovalDto;
import konopka.gerrit.data.entities.ChangeDto;
import konopka.gerrit.data.entities.CommentDto;
import konopka.gerrit.data.entities.PatchSetDto;
import konopka.gerrit.data.entities.PatchSetFileDto;
import konopka.util.Logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


public class ChangesRepository
        extends Repository
        implements IChangesRepository
{
    private static final Logger logger = LoggerFactory.getLogger(ChangesRepository.class);

    // language=SQL
    private static final String CREATE_TABLE_CHANGE = "CREATE TABLE IF NOT EXISTS `Change` (" +
            "Id                INT          NOT NULL," +
            "ProjectId         INT          NOT NULL," +
            "BranchId          INT                  ," +
            "ChangeId          VARCHAR(255) NOT NULL," +
            "OwnerId           INT          NOT NULL," +
            "NumberOfPatchSets INT          NOT NULL," +
            "Topic             TEXT                 ," +
            "Subject           TEXT                 ," +
            "CreatedAt         DATETIME     NOT NULL," +
            "UpdatedAt         DATETIME     NOT NULL," +
            "State             VARCHAR(255) NOT NULL," +
            "IsMergeable       BOOL         NOT NULL," +
            "BaseChangeId      INT                  ," +
            "CurrentPatchSetId VARCHAR(255)         , " +
            "PRIMARY KEY (Id)," +
            "FOREIGN KEY (ProjectId) REFERENCES Project(Id)," +
            "FOREIGN KEY (BranchId)  REFERENCES Branch(Id) ," +
            "FOREIGN KEY (OwnerId)   REFERENCES Account(Id)" +
            ')';


    private static final String INSERT_CHANGE_QUERY = "INSERT INTO `Change` (" +
            "Id, ProjectId, BranchId, ChangeId, OwnerId, NumberOfPatchSets," +
            "Topic, Subject, CreatedAt, UpdatedAt, State, IsMergeable, BaseChangeId, CurrentPatchSetId)" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_CHANGE_ID_QUERY = "SELECT Id FROM `Change` WHERE Id = ?";

    // language=SQL
    private static final String CREATE_TABLE_PATCH_SET = "CREATE TABLE IF NOT EXISTS PatchSet (" +
            "ChangeId      INT           NOT NULL," +
            "PatchSetId    VARCHAR(255)  NOT NULL," + // Compound key - identity
            "Number        INT           NOT NULL," +
            "GitCommitId   VARCHAR(255)  NOT NULL," + // key in set
            "Subject       VARCHAR(4000) NOT NULL," +
            "Message       TEXT          NOT NULL," +
//            "IsDraft       BOOL          NOT NULL, " +
            "CreatedAt     TIMESTAMP     NOT NULL," +
            "NumberOfFiles INT           NOT NULL," +
            "AuthorId      INT                   ," +
            "CommitterId   INT                   ," +
            "AddedLines    INT           NOT NULL, " +
            "DeletedLines  INT           NOT NULL," +
            "Ref           VARCHAR(255)," +
            "FOREIGN KEY (AuthorId)    REFERENCES Account(Id)," +
            "FOREIGN KEY (CommitterId) REFERENCES Account(Id)," +
            "FOREIGN KEY (ChangeId)    REFERENCES `Change`(Id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ')';


    private static final String INSERT_PATCHSET_QUERY = "INSERT INTO PatchSet " +
            "(ChangeId, PatchSetId, Number, GitCommitId, Subject, Message," +
            "CreatedAt, NumberOfFiles, AuthorId, CommitterId, AddedLines, DeletedLines, Ref) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";


    // language=SQL
    private static final String CREATE_TABLE_PATCH_SET_PARENT = "CREATE TABLE IF NOT EXISTS PatchSetParent (" +
            "ChildGitCommitId VARCHAR(255) NOT NULL," +
            "ParentGitCommitId VARCHAR(255) NOT NULL" +
            ')';

    private static final String INSERT_PATCH_SET_PARENT_QUERY = "INSERT INTO PatchSetParent " + "(ChildGitCommitId, " +
            "ParentGitCommitId) VALUES(?, ?)";

    // language=SQL
    private static final String CREATE_TABLE_PATCH_SET_APPROVAL = "CREATE TABLE IF NOT EXISTS ChangeApproval (" +
            "ValueTypeId int          NOT NULL," +
            "ValueId     smallint     NOT NULL," +
            "ChangeId    int          NOT NULL, " +
            "PatchSetId  VARCHAR(255)         ," +
            "CommentId   VARCHAR(255)         ," +
            "FOREIGN KEY (ValueTypeId, ValueId) REFERENCES ProjectApprovalValue(TypeId, Value)," +
            "FOREIGN KEY (ChangeId)             REFERENCES `Change`(ID) ON DELETE CASCADE ON UPDATE CASCADE" +
            ')';

    private static final String INSERT_INTO_PATCH_SET_APPROVAL = "INSERT INTO ChangeApproval " +
            "(ValueTypeId, ValueId, ChangeId, PatchSetId, CommentId) VALUES(?, ?, ?, ?, ?);";

    // language=SQL
    private static final String CREATE_TABLE_PATCH_SET_FILE = "CREATE TABLE IF NOT EXISTS PatchSetFile (" +
            "ChangeId       int NOT NULL," +
            "PatchSetId     VARCHAR(255) NOT NULL," +
            "PatchSetFileId VARCHAR(255) NOT NULL," +
            "Path           VARCHAR(255) NOT NULL," +
            "AddedLines     int NOT NULL, " +
            "DeletedLines   int NOT NULL," +
            //   "ChangeType VARCHAR(255) NOT NULL" +
            "IsBinary       BOOL NOT NULL," +
            //            "OldPath VARCHAR(255) NULL" +
            "FOREIGN KEY (ChangeId) REFERENCES `Change`(Id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ')';

    private static final String INSERT_PATCH_SET_FILE_QUERY = "INSERT INTO PatchSetFile " +
            "(ChangeId, PatchSetId, PatchSetFileId, Path, AddedLines," + "DeletedLines, IsBinary) " + // ChangeType ,
            // OldPath) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?);";

    // language=SQL
    private static final String CREATE_TABLE_COMMENTS = "CREATE TABLE IF NOT EXISTS Comment(" +
            "ChangeId int NOT NULL," +
            "PatchSetId VARCHAR(255) NULL," +
            "PatchSetFileId VARCHAR(255) NULL," +
            "CommentId VARCHAR(255) NOT NULL," +
            "IsRevisionMessage bit NOT NULL," +
            "CreatedAt datetime NOT NULL," +
            "Message LONGTEXT NOT NULL," +
            "ReplyToCommentId VARCHAR(255) NULL," +
            "AuthorId int NULL," +
            "Line int NULL," +
            "IsRange bit NULL," +
            "RangeStartLine int NULL," +
            "RangeStartCharacter int NULL," +
            "RangeEndLine int NULL," +
            "RangeEndCharacter int NULL," +
            "FOREIGN KEY (AuthorId) REFERENCES Account(Id)," +
            "FOREIGN KEY (ChangeId) REFERENCES `Change`(Id) ON DELETE CASCADE ON UPDATE CASCADE" +
    ')';

    private static final String INSERT_PATCH_SET_MESSAGE_QUERY = "INSERT INTO Comment " +
            "(ChangeId, PatchSetId, CommentId," + "CreatedAt, Message, AuthorId, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, ?, 1)";
    private static final String INSERT_CHANGE_MESSAGE_QUERY = "INSERT INTO Comment " +
            "(ChangeId, CommentId," + "CreatedAt, Message, AuthorId, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, 1)";
    private static final String INSERT_LINE_COMMENT_QUERY = "INSERT INTO Comment " +
            "(ChangeId, PatchSetId, PatchSetFileId, CommentId, " +
            "CreatedAt, Message, ReplyToCommentId, AuthorId, " +
            "Line, IsRange, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0);";
    private static final String INSERT_RANGE_COMMENT_QUERY = "INSERT INTO Comment " +
            "(ChangeId, PatchSetId, PatchSetFileId, CommentId, " +
            "CreatedAt, Message, ReplyToCommentId, AuthorId, " +
            "RangeStartLine, RangeStartCharacter, RangeEndLine," +
            "RangeEndCharacter, IsRange, IsRevisionMessage) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0);";


    private final Connection connection;


    ChangesRepository(Connection connection)
    {
        this.connection = connection;
    }


    @Override
    public void init() throws SQLException
    {
        executeSqlStatement(connection, CREATE_TABLE_CHANGE);
        executeSqlStatement(connection, CREATE_TABLE_PATCH_SET);
        executeSqlStatement(connection, CREATE_TABLE_PATCH_SET_PARENT);
        executeSqlStatement(connection, CREATE_TABLE_PATCH_SET_APPROVAL);
        executeSqlStatement(connection, CREATE_TABLE_PATCH_SET_FILE);
        executeSqlStatement(connection, CREATE_TABLE_COMMENTS);
    }

    private String buildPatchSetId(PatchSetDto patchSet)
    {
        return patchSet.change.id + "-" + patchSet.number;
    }

    private String buildPatchSetFileId(PatchSetFileDto file)
    {
        return buildPatchSetId(file.patchSet) + "-" + file.path;
    }

    @Override
    public void addChange(ChangeDto change) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_CHANGE_QUERY))
        {
            stmt.setInt(1, change.id);
            stmt.setInt(2, change.project.id);
            if (change.branch != null)
            {
                stmt.setInt(3, change.branch.id);
            }
            else
            {
                stmt.setNull(3, Types.INTEGER);
                logger.info(Logging.prepareWithPart("addChange", "null branch", Integer.toString(change.id)));
            }
            stmt.setString(4, change.changeId);
            stmt.setInt(5, change.owner.id);
            stmt.setInt(6, change.getNumberOfPatchSets());
            stmt.setString(7, change.topic);
            stmt.setString(8, change.subject);
            stmt.setTimestamp(9, change.createdAt);
            stmt.setTimestamp(10, change.updatedAt);
            stmt.setString(11, change.state.name().toUpperCase());
            stmt.setBoolean(12, change.isMergeable != null ? change.isMergeable : false);
            stmt.setString(13, change.baseChangeId);
            if (change.currentPatchSet != null)
            {
                stmt.setString(14, buildPatchSetId(change.currentPatchSet));
            }
            else
            {
                stmt.setString(14, null);
            }

            stmt.execute();

        }

        for (PatchSetDto patchSet : change.patchSets)
        {
            addPatchSet(patchSet);
        }

        for (ChangeApprovalDto approval : change.approvals)
        {
            addChangeApproval(approval);
        }

        for (CommentDto comment : change.comments)
        {
            addComment(comment);
        }

    }

    public boolean containsChange(int id) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_CHANGE_ID_QUERY))
        {
            stmt.setInt(1, id);
            ResultSet results = stmt.executeQuery();
            return results.next();
        }
    }

    private void addPatchSet(PatchSetDto patch) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PATCHSET_QUERY))
        {
            stmt.setInt(1, patch.change.id);
            stmt.setString(2, buildPatchSetId(patch));
            stmt.setInt(3, patch.number);
            stmt.setString(4, patch.gitCommitId);
            stmt.setString(5, patch.subject);
            stmt.setString(6, patch.message);
            stmt.setTimestamp(7, patch.createdAt);
            stmt.setInt(8, patch.getNumberOfFiles());
            stmt.setInt(9, patch.author.id);
            stmt.setInt(10, patch.committer.id);
            stmt.setInt(11, patch.addedLines);
            stmt.setInt(12, patch.deletedLines);
            stmt.setString(13, patch.ref);

            stmt.execute();

            for (String parent : patch.parents)
            {
                addPatchSetParent(patch.gitCommitId, parent);
            }

            for (PatchSetFileDto file : patch.files)
            {
                addPatchSetFile(file);
            }
        }
    }

    private void addPatchSetParent(String child, String parent) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PATCH_SET_PARENT_QUERY))
        {
            stmt.setString(1, child);
            stmt.setString(2, parent);
            stmt.execute();
        }
    }

    private void addComment(CommentDto comment) throws SQLException
    {
        if (comment.patchSetFile != null)
        {
            if (comment.range != null)
            {
                addPatchSetFileRangeComment(comment);
            }
            else
            {
                addPatchSetFileLineComment(comment);
            }
        }
        else if (comment.patchSet != null)
        {
            addPatchSetMessage(comment);
        }
        else
        {
            addChangeMessage(comment);
        }
    }

    private void addChangeApproval(ChangeApprovalDto approval) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_INTO_PATCH_SET_APPROVAL))
        {
            stmt.setInt(1, approval.type.id);
            stmt.setShort(2, approval.value.value);
            stmt.setInt(3, approval.change.id);
            stmt.setString(4, buildPatchSetId(approval.comment.patchSet));
            stmt.setString(5, approval.comment.id);
            stmt.execute();
        }
    }

    private void addChangeMessage(CommentDto comment) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_CHANGE_MESSAGE_QUERY))
        {
            stmt.setInt(1, comment.change.id);
            stmt.setString(2, comment.id);
            stmt.setTimestamp(3, comment.createdAt);
            stmt.setString(4, comment.message);
            if (comment.author != null)
            {
                stmt.setInt(5, comment.author.id);
            }
            else
            {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.execute();
        }
    }

    private void addPatchSetMessage(CommentDto comment) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PATCH_SET_MESSAGE_QUERY))
        {
            stmt.setInt(1, comment.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(comment.patchSet));
            stmt.setString(3, comment.id);
            stmt.setTimestamp(4, comment.createdAt);
            stmt.setString(5, comment.message);
            if (comment.author != null)
            {
                stmt.setInt(6, comment.author.id);
            }
            else
            {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.execute();
        }
    }

    private void addPatchSetFileLineComment(CommentDto comment) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_LINE_COMMENT_QUERY))
        {
            stmt.setInt(1, comment.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(comment.patchSet));
            stmt.setString(3, buildPatchSetFileId(comment.patchSetFile));
            stmt.setString(4, comment.id);
            stmt.setTimestamp(5, comment.createdAt);

            stmt.setString(6, comment.message);
            stmt.setString(7, comment.inReplyToCommentId);

            if (comment.author != null)
            {
                stmt.setInt(8, comment.author.id);
            }
            else
            {
                stmt.setNull(8, Types.INTEGER);
            }
            if (comment.line != null)
            {
                stmt.setInt(9, comment.line);
            }
            else
            {
                stmt.setNull(9, Types.INTEGER);
            }

            stmt.execute();
        }
    }

    private void addPatchSetFileRangeComment(CommentDto comment) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_RANGE_COMMENT_QUERY))
        {
            stmt.setInt(1, comment.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(comment.patchSet));
            stmt.setString(3, buildPatchSetFileId(comment.patchSetFile));
            stmt.setString(4, comment.id);
            stmt.setTimestamp(5, comment.createdAt);

            stmt.setString(6, comment.message);
            stmt.setString(7, comment.inReplyToCommentId);

            if (comment.author != null)
            {
                stmt.setInt(8, comment.author.id);
            }
            else
            {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setInt(9, comment.range.startLine);
            stmt.setInt(10, comment.range.startCharacter);
            stmt.setInt(11, comment.range.endLine);
            stmt.setInt(12, comment.range.endCharacter);
            stmt.execute();
        }
    }

    private void addPatchSetFile(PatchSetFileDto file) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PATCH_SET_FILE_QUERY))
        {
            stmt.setInt(1, file.patchSet.change.id);
            stmt.setString(2, buildPatchSetId(file.patchSet));
            stmt.setString(3, buildPatchSetFileId(file));
            stmt.setString(4, file.path);
            stmt.setInt(5, file.addedLines);
            stmt.setInt(6, file.deletedLines);
            stmt.setBoolean(7, file.isBinary);//changeType.name().toUpperCase());
            //            stmt.setBoolean(8, file.isBinary);
            //            stmt.setString(9, file.oldPath);

            stmt.execute();
        }
    }
}
