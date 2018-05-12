package konopka.gerrit.data.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ApprovalTypeDto
{
    public final ProjectDto project;
    public final short defaultValue;
    public final String name;
    public final List<ApprovalValueDto> values;
    public int id;


    public ApprovalTypeDto(ProjectDto project, String name, short defaultValue, ApprovalValueDto... values)
    {
        this.project = project;
        this.name = name;
        this.defaultValue = defaultValue;
        this.values = Arrays.asList(values);
    }

    //    public ApprovalTypeDto(int id, ProjectDto project, String name, short defaultValue, ApprovalValueDto... values) {
    //        this(project, name, defaultValue, values);
    //        this.id = id;
    //    }

    public ApprovalTypeDto(int id, ProjectDto project, String name, short defaultValue, List<ApprovalValueDto> values)
    {
        this.id = id;
        this.project = project;
        this.name = name;
        this.defaultValue = defaultValue;
        this.values = new ArrayList<>(values);
    }
}

