package mtmc.os.fs;

import java.io.File;
import java.util.*;

public class Listing {
    public final String path;
    public final String name;
    public final ArrayList<File> listOfFiles;
    public final LinkedHashMap<String, Listing> subdirectories;


    public Listing(String path, String name, ArrayList<File> listOfFiles, LinkedHashMap<String, Listing> subdirectories) {
        this.path = path.replace('\\', '/');
        this.name = name;
        this.listOfFiles = listOfFiles;
        this.subdirectories = subdirectories;
    }
}
