package mtmc.os.fs;

import java.io.File;
import java.util.*;

public class Listing {
    public final String path;
    public final String name;
    public final List<File> listOfFiles;
    public final LinkedHashMap<String, Listing> subdirectories;
    
    private FileSystem fs;
    private File file;

    public Listing(FileSystem fs, File file) {
        String root = FileSystem.DISK_PATH.toFile().getAbsolutePath().replace('\\', '/');
        String path = file.getAbsolutePath().substring(root.length()).replace('\\', '/');

        this.fs = fs;
        this.file = file;
        
        this.path = path.length() > 0 ? path : "/";
        this.name = path.length() > 0 ? file.getName() : "/";
        this.listOfFiles = Arrays.asList(file.listFiles(child -> !child.isDirectory()));
        this.subdirectories = new LinkedHashMap<>();
        
        for (File child : file.listFiles()) {
            if(!child.isDirectory()) continue;
            
            this.subdirectories.put(child.getName(), new Listing(fs, child));
        }
    }
}
