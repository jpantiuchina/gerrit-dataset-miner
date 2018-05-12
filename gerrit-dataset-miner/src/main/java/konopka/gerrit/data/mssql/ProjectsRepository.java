package konopka.gerrit.data.mssql;

import konopka.gerrit.data.IProjectsRepository;
import konopka.gerrit.data.Repository;
import konopka.gerrit.data.entities.ApprovalTypeDto;
import konopka.gerrit.data.entities.ApprovalValueDto;
import konopka.gerrit.data.entities.BranchDto;
import konopka.gerrit.data.entities.ProjectDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectsRepository
        extends Repository
        implements IProjectsRepository
{
    // language=SQL
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS Project (" +
            "Id        INT AUTO_INCREMENT NOT NULL ," +
            "ProjectId VARCHAR(255)       NOT NULL," +
            "Name      VARCHAR(255)       NOT NULL," +
            "ParentId  INT                        ," +
            "PRIMARY   KEY (Id)," +
            "FOREIGN KEY (ParentId) REFERENCES Project (Id) " +
    ')';

    // language=SQL
    private static final String CREATE_BRANCH_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS Branch (" +
            "Id        INT AUTO_INCREMENT NOT NULL," +
            "ProjectId INT                NOT NULL," +
            "Name      VARCHAR(255) NOT NULL," +
            "Revision VARCHAR(255) NOT NULL," +
            "PRIMARY KEY (Id)," +
            "FOREIGN KEY (ProjectId) REFERENCES Project(Id) ON DELETE CASCADE ON UPDATE CASCADE" +
    ')';

    // language=SQL
    private static final String CREATE_APPROVAL_TYPE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS ProjectApprovalType (" +
            "Id        int AUTO_INCREMENT NOT NULL, " +
            "ProjectId int                NOT NULL," +
            "Name VARCHAR(255) NOT NULL," +
            "DefaultValue smallint NOT NULL," +
            "PRIMARY KEY (Id)," +
            "FOREIGN KEY (ProjectId) REFERENCES Project(Id) ON DELETE CASCADE ON UPDATE CASCADE" +
    ')';

    private static final String CREATE_APPROVAL_VALUE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS ProjectApprovalValue (" +
            "TypeId int NOT NULL, " +
            "Value smallint NOT NULL," +
            "Description TEXT NOT NULL," +
            "PRIMARY KEY ( TypeId, Value )," +
            "FOREIGN KEY (TypeId) REFERENCES ProjectApprovalType (Id) ON DELETE CASCADE ON UPDATE CASCADE" +
    ')';

    private static final String INSERT_APPROVAL_TYPE_QUERY = "INSERT INTO ProjectApprovalType" + "(ProjectId, Name, DefaultValue) Values(?, ?, ?);";

    private static final String INSERT_APPROVAL_VALUE_QUERY = "INSERT INTO ProjectApprovalValue " + "(TypeId, Value, Description) VALUES(?, ?, ?);";


    private static final String INSERT_PROJECT_QUERY = "INSERT INTO Project(" + "ProjectId, " + "Name, " + "ParentId" + ") VALUES(?, ?, ?);";


    private static final String SELECT_PROJECTS_QUERY = "SELECT Id, ProjectId, Name, ParentId FROM Project";
    private static final String SELECT_BRANCHES_QUERY = "SELECT Id, Name, Revision FROM Branch WHERE ProjectId = ?";
    private static final String SELECT_APPROVAL_TYPES_QUERY = "SELECT Id, Name, DefaultValue FROM ProjectApprovalType WHERE ProjectId = ?";
    private static final String SELECT_APPROVAL_VALUES_QUERY = "SELECT Value, Description FROM ProjectApprovalValue WHERE TypeId = ?";
    private static final String INSERT_BRANCH_QUERY = "INSERT INTO Branch (ProjectId, Name, Revision) VALUES(?,?,?)";

    private final Connection connection;



    ProjectsRepository(Connection connection)
    {
        this.connection = connection;
    }



    @Override
    public void init() throws SQLException
    {

        executeSqlStatement(connection, CREATE_TABLE_QUERY);
        executeSqlStatement(connection, CREATE_BRANCH_TABLE_QUERY);
        executeSqlStatement(connection, CREATE_APPROVAL_TYPE_TABLE_QUERY);
        executeSqlStatement(connection, CREATE_APPROVAL_VALUE_TABLE_QUERY);
    }

    @Override
    public List<ProjectDto> getAllProjects() throws SQLException
    {
        try (Statement stmt = connection.createStatement())
        {
            try (ResultSet results = stmt.executeQuery(SELECT_PROJECTS_QUERY))
            {
                List<ProjectDto> projects = new ArrayList<>();

                while (results.next())
                {
                    ProjectDto project = new ProjectDto(results.getInt("Id"), results.getString("ProjectId"), results.getString("Name"));

                    project.parentId = Optional.of(results.getInt("ParentId"));
                    if (results.wasNull())
                    {
                        project.parentId = Optional.empty();
                    }

                    //getBranches(project).forEach(project.branches::add);
                    getApprovals(project).forEach(a -> project.approvals.put(a.name, a));
                    projects.add(project);
                }

                return projects;
            }
        }
    }



    @Override
    public void loadProjectBranches(ProjectDto project) throws SQLException
    {
        project.branches.addAll(getBranches(project));
    }



    @Override
    public List<BranchDto> getBranches(ProjectDto project) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_BRANCHES_QUERY))
        {
            stmt.setInt(1, project.id);
            try (ResultSet results = stmt.executeQuery())
            {
                List<BranchDto> branches = new ArrayList<>();

                while (results.next())
                {
                    BranchDto branch = new BranchDto();
                    branch.id = results.getInt("Id");
                    branch.name = results.getString("Name");
                    branch.revision = results.getString("Revision");
                    branch.projectId = project.id;

                    branches.add(branch);
                }

                return branches;
            }
        }
    }



    private List<ApprovalTypeDto> getApprovals(ProjectDto project) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_APPROVAL_TYPES_QUERY))
        {
            stmt.setInt(1, project.id);

            try (ResultSet results = stmt.executeQuery())
            {
                List<ApprovalTypeDto> approvals = new ArrayList<>();

                while (results.next())
                {
                    int id = results.getInt("Id");
                    List<ApprovalValueDto> values = getApprovalValues(id);

                    ApprovalTypeDto approval = new ApprovalTypeDto(results.getInt("Id"), project, results.getString("Name"), results.getShort("DefaultValue"), values);

                    approvals.add(approval);
                }

                return approvals;
            }
        }
    }



    private List<ApprovalValueDto> getApprovalValues(int typeId) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_APPROVAL_VALUES_QUERY))
        {
            stmt.setInt(1, typeId);
            try (ResultSet results = stmt.executeQuery())
            {
                List<ApprovalValueDto> values = new ArrayList<>();

                while (results.next())
                {
                    ApprovalValueDto value = new ApprovalValueDto(results.getShort("Value"), results.getString("Description"));
                    values.add(value);
                }

                return values;
            }
        }
    }



    @Override
    public ProjectDto add(ProjectDto project) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PROJECT_QUERY, Statement.RETURN_GENERATED_KEYS))
        {
            stmt.setString(1, project.projectId);
            stmt.setString(2, project.name);

            if (project.parentId.isPresent())
            {
                stmt.setInt(3, project.parentId.get());
            } else
            {
                stmt.setNull(3, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0)
            {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys())
                {
                    if (generatedKeys.next())
                    {
                        project.id = generatedKeys.getInt(1);
                    }
                }
            }
            return project;
        }
    }



    @Override
    public BranchDto addBranch(ProjectDto project, String branch, String revision) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_BRANCH_QUERY, Statement.RETURN_GENERATED_KEYS))
        {
            stmt.setInt(1, project.id);
            stmt.setString(2, branch);
            stmt.setString(3, revision);

            BranchDto branchdto = new BranchDto();
            branchdto.name = branch;
            branchdto.revision = revision;
            branchdto.projectId = project.id;

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0)
            {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys())
                {
                    if (generatedKeys.next())
                    {
                        branchdto.id = generatedKeys.getInt(1);
                    }
                }
            }

            return branchdto;
        }
    }



    @Override
    public ApprovalTypeDto addApprovalType(ApprovalTypeDto approvalTypeDto) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_APPROVAL_TYPE_QUERY, Statement.RETURN_GENERATED_KEYS))
        {
            stmt.setInt(1, approvalTypeDto.project.id);
            stmt.setString(2, approvalTypeDto.name);
            stmt.setShort(3, approvalTypeDto.defaultValue);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0)
            {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys())
                {
                    if (generatedKeys.next())
                    {
                        approvalTypeDto.id = generatedKeys.getInt(1);
                    }
                }
            }

            for (ApprovalValueDto v : approvalTypeDto.values)
            {
                addApprovalValue(approvalTypeDto.id, v);
            }
            return approvalTypeDto;
        }
    }



    private void addApprovalValue(int id, ApprovalValueDto value) throws SQLException
    {
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_APPROVAL_VALUE_QUERY, Statement.RETURN_GENERATED_KEYS))
        {
            stmt.setInt(1, id);
            stmt.setShort(2, value.value);
            stmt.setString(3, value.description);

            stmt.execute();
        }
    }


}
