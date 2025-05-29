package mtmc.os.fs;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class Listing {
    public final File path;
    public final ArrayList<File> listOfFiles;
    public final LinkedHashMap<String, Listing> subdirectories;


    public Listing(File path, ArrayList<File> listOfFiles, LinkedHashMap<String, Listing> subdirectories) {
        this.path = path;
        this.listOfFiles = listOfFiles;
        this.subdirectories = subdirectories;
    }
}
