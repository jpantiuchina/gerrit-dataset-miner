package konopka.gerrit.data.entities;

public class PatchSetFileDto
{
    public PatchSetDto patchSet;
    public String path;
    public int addedLines;
    public int deletedLines;
    //public ChangeType changeType;
    public boolean isBinary;
    public String oldPath;

    public PatchSetFileDto(PatchSetDto patchSet)
    {
        this.patchSet = patchSet;
    }
}
