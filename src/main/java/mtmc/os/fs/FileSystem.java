package mtmc.os.fs;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.javalin.http.Context;
import mtmc.web.WebServer;

public class FileSystem {
    private static Map<String, ArrayList<String>> DIRECTORY_W_FILES = new TreeMap<String, ArrayList<String>>();
    static final Path DISK_PATH = Path.of(System.getProperty("user.dir"), "disk").toAbsolutePath();
    static final Path HOME_PATH = Path.of(DISK_PATH.toString(), "/home");

    static Path getDiskPath(String pathString) { // C:\Users_username\folderParents\mtmc\disk
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

    public String resolve(String fileName) {
        ArrayList<String> resolvedArr = new ArrayList<>();
        String fileString = "";
        System.out.println("New Test");
        String[] parts = fileName.split("/");
        System.out.println(List.of(parts)); // Input Path
        if (parts.length == 0) {
            return null;
        }
        if (parts[0].equals("")) { // First is empty string = Absolute
            System.out.println("ABSOLUTE");
            // Creates a resolved path array

            for (int dirVar = 0; dirVar < parts.length; dirVar++) { // Remember parts[0].equals("")
                if (parts[dirVar] == "") {
                } else if (parts[dirVar].equals(".")) {
                } else if (parts[dirVar].equals("..")) { // Removes the last added directory parent
                    if (!resolvedArr.isEmpty()) {
                        resolvedArr.removeLast();
                    }
                } else if (parts[dirVar] != "..") {
                    resolvedArr.add(parts[dirVar]);
                }
            }
            System.out.println(resolvedArr);
        }
        if (!parts[0].equals("")) { // Checks for empty string
            System.out.println("RELATIVE");

        }

        // Converting path arrays into strings
        if (resolvedArr.size() != 0){
            if (resolvedArr.getFirst().equals("home"))
                for (int resolvedDirs = 0; resolvedDirs < resolvedArr.size(); resolvedDirs++) {
                    fileString += ("/" + resolvedArr.get(resolvedDirs));
                }
            else if (!resolvedArr.getFirst().equals("home")) {
                for (int resolvedDirs = 0; resolvedDirs < resolvedArr.size(); resolvedDirs++) {
                    fileString += ("/" + resolvedArr.get(resolvedDirs));
                }
            }
        }
        System.out.println("fileString: " + fileString);
        System.out.println("\n");
        return fileString;
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

