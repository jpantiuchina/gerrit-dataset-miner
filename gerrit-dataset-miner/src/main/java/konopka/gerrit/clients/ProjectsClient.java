package konopka.gerrit.clients;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.projects.BranchInfo;
import com.google.gerrit.extensions.api.projects.ProjectApi;
import com.google.gerrit.extensions.common.LabelInfo;
import com.google.gerrit.extensions.common.ProjectInfo;
import konopka.gerrit.data.IProjectsRepository;
import konopka.gerrit.data.cache.ProjectsCache;
import konopka.gerrit.data.entities.ApprovalTypeDto;
import konopka.gerrit.data.entities.ApprovalValueDto;
import konopka.gerrit.data.entities.BranchDto;
import konopka.gerrit.data.entities.ProjectDto;
import konopka.util.Logging;

import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


public class ProjectsClient
{
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ProjectsClient.class);
    public static final ApprovalValueDto[] EMPTY_APPROVAL_VALUE_DTO_ARRAY = new ApprovalValueDto[0];


    private GerritApi api;
    private IProjectsRepository repo;
    private ProjectsCache cache;
    private Boolean downloadParents;
    private WaitCaller caller;

    public ProjectsClient(GerritApi api, WaitCaller caller, IProjectsRepository repository, Boolean downloadParents)
    {
        this.api = api;
        this.repo = repository;
        this.caller = caller;

        this.cache = new ProjectsCache();
        this.downloadParents = downloadParents;
    }

    public void prepare() throws SQLException
    {
        cache.restore(repo.getAllProjects());
    }


    public ProjectDto getProject(String id) throws SQLException
    {
        if (cache.isCached(id))
        {
            return cache.tryGetCached(id);
        }

        ProjectInfo info = null;

        try
        {
            info = caller.waitOrCall(() -> api.projects().name(id).get());
        }
        catch (Exception e)
        {

            logger.error(Logging.prepare("getProject", id), e);
        }

        if (info != null)
        {
            ProjectDto project = new ProjectDto(info.id, info.name);

            if (info.parent != null && downloadParents)
            {
                ProjectDto parent = getProject(info.parent);
                project.parentId = Optional.of(parent.id);
            }

            project = repo.add(project);

            // getProjectBranches(project);
            // getApprovals(project);

            cache.cache(project);

            return project;
        }

        return null;
    }

    //
    //    private void getApprovals(ProjectDto project) {
    //        int start = 0;
    //        int limit = 1;
    //
    //        boolean getCommands = true;
    //
    //        List<ApprovalTypeDto> approvals = new ArrayList<>();
    //        while (approvals.size() <= 0) {
    //            List<ChangeInfo> changes = null;
    //
    //            try {
    //                Changes.QueryRequest request = api.changes().query()
    //                        .withStart(start)
    //                        .withLimit(limit)
    //                        .withOptions(ListChangesOption.DETAILED_LABELS);
    //                if (getCommands) {
    //                    request.withOption(ListChangesOption.DOWNLOAD_COMMANDS);
    //                }
    //
    //                changes = caller.waitOrCall(() -> request.get());
    //            } catch (Exception e) {
    //                e.printStackTrace();
    //                getCommands = false;
    //            }
    //
    //            if (changes != null && changes.size() > 0) {
    //                ChangeInfo change = changes.get(0);
    //
    //                if (change.labels != null && change.labels.size() > 0) {
    //                    approvals.addAll(createApprovalTypes(project, change.labels));
    //                }
    //
    //            }
    //
    //            start += limit;
    //        }
    //
    //        approvals.forEach(repo::addApprovalType);
    //
    //        approvals.forEach(a -> project.approvals.put(a.name, a));
    //    }

    private List<ApprovalTypeDto> createApprovalTypes(ProjectDto project, Map<String, LabelInfo> labels)
    {
        return labels.entrySet().stream().filter(e -> project.approvals.containsKey(e.getKey()) == false).map(e ->
                createApprovalType(project, e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    private ApprovalTypeDto createApprovalType(ProjectDto project, String type, LabelInfo label)
    {
        short defaultValue = 0;
        if (label != null && label.defaultValue != null)
        {
            defaultValue = label.defaultValue;
        }

        ApprovalValueDto[] labelValues;
        if (label.values != null)
        {
            labelValues = label.values.entrySet().stream()
                .map(ev -> new ApprovalValueDto(ev.getKey(), ev.getValue()))
                .toArray(ApprovalValueDto[]::new);
        }
        else
        {
            labelValues = EMPTY_APPROVAL_VALUE_DTO_ARRAY;
        }

        return new ApprovalTypeDto(project, type, defaultValue, labelValues);
    }

    public void addApprovals(ProjectDto project, Map<String, LabelInfo> labels) throws SQLException
    {
        for (Map.Entry<String, LabelInfo> e : labels.entrySet())
        {
            addApproval(project, e.getKey(), e.getValue());
        }
    }

    public void addApproval(ProjectDto project, String key, LabelInfo label) throws SQLException
    {
        if (!project.approvals.containsKey(key))
        {
            ApprovalTypeDto approval = createApprovalType(project, key, label);
            repo.addApprovalType(approval);
            project.approvals.put(approval.name, approval);
        }
    }

    private void getProjectBranches(ProjectDto project)
    {
        List<BranchInfo> branches = null;
        try
        {
            ProjectApi.ListBranchesRequest request = api.projects().name(project.projectId).branches();
            branches = caller.waitOrCall(request::get);
        }
        catch (Exception e)
        {
            logger.error(Logging.prepare("getProjectBranches", project.projectId), e);

        }

        if (branches != null)
        {
            List<BranchDto> projectBranches = branches.stream().
                    filter(b -> !project.hasBranch(b.ref)).
                    map(b -> {
                        try
                        {
                            return repo.addBranch(project, b.ref, b.revision);
                        }
                        catch (SQLException e)
                        {
                            throw new RuntimeException(e);
                        }
                    }).
                    filter(Objects::nonNull).
                    collect(Collectors.toList());

            project.branches.addAll(projectBranches);
        }
    }

    public BranchDto getBranch(ProjectDto project, String name) throws SQLException
    {
        if (project.hasBranch(name) == false)
        {
            repo.loadProjectBranches(project);
        }

        if (project.hasBranch(name) == false)
        {
            getProjectBranches(project);
        }
        return project.getBranch(name);

    }
}
