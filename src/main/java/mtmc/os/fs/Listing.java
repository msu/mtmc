package mtmc.os.fs;

import java.io.File;
import java.util.*;

public class Listing {
    public final String path;
    public final String name;
    public final boolean directory;
    public final boolean root;
    
    private FileSystem fs;
    private File file;

    public Listing(FileSystem fs, File file) {
        String root = FileSystem.DISK_PATH.toFile().getAbsolutePath().replace('\\', '/');
        String path = file.getAbsolutePath().substring(root.length()).replace('\\', '/');

        this.fs = fs;
        this.file = file;
        
        this.path = path.length() > 0 ? path : "/";
        this.name = path.length() > 0 ? file.getName() : "/";
        this.directory = file.isDirectory();
        this.root = this.path.equals("/");
    }
    
    public Listing getParent() {
        return new Listing(fs, file.getParentFile());
    }
    
    public List<Listing> list() {
        if (!directory) {
            return new ArrayList<>();
        }
        
        var list = new ArrayList<Listing>();
        var children = file.listFiles();
                
        Arrays.sort(children, (a, b) -> {
            return a.getName().compareTo(b.getName());
        });
        
        for (File child : children) {
            list.add(new Listing(fs, child));
        }
        
        return list;
    }
}
