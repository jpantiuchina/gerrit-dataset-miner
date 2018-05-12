package konopka.gerrit.data.entities;

import com.google.gerrit.extensions.client.ChangeStatus;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class ChangeDto
{
    public int id; // number
    public String changeId; // naturalId
    public ProjectDto project;
    public BranchDto branch;
    public AccountDto owner;
    public String topic;
    public String subject;
    public Timestamp createdAt;
    public Timestamp updatedAt;
    public ChangeStatus state;
    public Boolean isMergeable;
    public String baseChangeId;
    public PatchSetDto currentPatchSet; // foreign
    public List<PatchSetDto> patchSets;


    //  public List<ApprovalDto> approvals;
    //  public List<CommentDto> comments;
    public List<ChangeApprovalDto> approvals;
    public List<CommentDto> comments;

    public ChangeDto(int id, ProjectDto project, BranchDto branch, AccountDto owner)
    {
        this.id = id;
        this.project = project;
        this.branch = branch;
        this.owner = owner;
        this.approvals = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.patchSets = new ArrayList<>();

    }

    public int getNumberOfPatchSets()
    {
        return patchSets.size();
    }

    //public List<ProblemDto> problems;
}


