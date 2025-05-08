package mtmc.os.fs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import io.javalin.http.Context;
import mtmc.web.WebServer;

public class FileSystem {
    private static Map<String, ArrayList<String>> DIRECTORY_W_FILES = new TreeMap<String, ArrayList<String>>();
    static final Path DISK_PATH = Path.of(System.getProperty("user.dir"), "disk").toAbsolutePath();

    static Path getDiskPath(String pathString) {
        Path path = Path.of(pathString);
        if (!path.isAbsolute()) {
            path = DISK_PATH.resolve(path);
        }
        path = path.toAbsolutePath();
        if (!path.startsWith(DISK_PATH)) {
            throw new IllegalArgumentException(pathString + " is not a disk path");
        }
        return path;
    }

    public void listFiles(String basepath) {
        Path TEST_PATH = getDiskPath(basepath);
        File[] filesInDir = new File(TEST_PATH.toUri()).listFiles();
        // getParent: normalize for shell like stuff
        for (File file : filesInDir) {
            System.out.println(file.getName());
        }
        //
        System.out.println("Should've printed the files.");
    }
    //TODO: Ensure relative absolute something:
    // Compare paths to $user/disk

       /*
    TODO: Create "directories" variable to iterate
    for (directory = 0; directory < directories.length; directory++) // Iterate through directories
        if (directory.isDirectory){
            return;
        }
        else if(!directory.isDirectory){ // If directory doesn't exist, add to TreeMap. Then iterate and add files within
            // TODO:
                - Create array of files within directory
                - Input array(files[directory.length] into TreeMap
                - TreeMap <directory, files>

            DIRECTORY_W_FILES.put(directory, TODO:ARRAY_VAR);
        } throws FileNotFoundException("File not found");

        public static boolean isDirectory(String directory) {return DIRECTORIES.containsKey(directory);}

     */

}

