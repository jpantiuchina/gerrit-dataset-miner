package konopka.gerrit.data;


import konopka.gerrit.data.entities.ApprovalTypeDto;
import konopka.gerrit.data.entities.BranchDto;
import konopka.gerrit.data.entities.ProjectDto;

import java.sql.SQLException;
import java.util.List;

public interface IProjectsRepository
        extends IRepository
{
    ProjectDto add(ProjectDto project) throws SQLException;

    BranchDto addBranch(ProjectDto project, String branch, String revision) throws SQLException;

    ApprovalTypeDto addApprovalType(ApprovalTypeDto approvalTypeDto) throws SQLException;

    List<ProjectDto> getAllProjects() throws SQLException;

    List<BranchDto> getBranches(ProjectDto project) throws SQLException;

    void loadProjectBranches(ProjectDto project) throws SQLException;
}
