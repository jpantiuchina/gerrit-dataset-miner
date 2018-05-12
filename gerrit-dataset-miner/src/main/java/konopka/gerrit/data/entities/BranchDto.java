package konopka.gerrit.data.entities;


public class BranchDto
{
    public int id;
    public String name;
    public String revision;
    public int projectId;

    public boolean equalsByName(String name)
    {
        return this.name.equals(name) || this.revision.equals(name) || (this.name.endsWith(name) && this.name.startsWith("refs")) || this.name.contains(name);
    }
}
