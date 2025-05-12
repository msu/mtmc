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

// TODO: stop using the Path objects, remove prints when not debugging
public class FileSystem {
    private static Map<String, ArrayList<String>> DIRECTORY_W_FILES = new TreeMap<String, ArrayList<String>>();
    private String cwd = "/home";
    // TODO: make these strings and write a `static String join(String path, String path)` method
    static final Path DISK_PATH = Path.of(System.getProperty("user.dir"), "disk").toAbsolutePath();
    static final Path HOME_PATH = Path.of(DISK_PATH.toString(), "/home");

    /*static Path getDiskPath(String pathString) { // C:\Users_username\folderParents\mtmc\disk
        Path path = Path.of(pathString);
        if (!path.isAbsolute()) {
            path = DISK_PATH.resolve(path);
        }
        path = path.toAbsolutePath();
        if (!path.startsWith(DISK_PATH)) {
            throw new IllegalArgumentException(pathString + " is not a disk path");
        }
        return path;
    }*/

    public void setCWD(String cd){
        cwd = cd;
    }
    /*public void listFiles(String basepath) {
        Path TEST_PATH = getDiskPath(basepath);
        File[] filesInDir = new File(TEST_PATH.toUri()).listFiles();
        // getParent: normalize for shell like stuff
        for (File file : filesInDir) {
            System.out.println(file.getName());
        }
        //
        System.out.println("Should've printed the files.");
    }*/

    public String resolve(String fileName) {
        ArrayList<String> resolvedArr = new ArrayList<>();
        String pathString = null;
        String[] parts = fileName.split("/");

        System.out.println("New Test");
        System.out.println(List.of(parts)); // Input Path

        if (parts.length == 0) {
            return null;
        }
        // TODO: maybe extract this duplicated loop
        // Absolute path case
        if (parts[0].equals("")) {
            System.out.println("ABSOLUTE");
            for (int dirVar = 0; dirVar < parts.length; dirVar++) {
                if (parts[dirVar].equals("") || parts[dirVar].equals(".")) {
                    continue;
                } else if (parts[dirVar].equals("..")) {
                    if (!resolvedArr.isEmpty()) {
                        resolvedArr.removeLast();
                    }
                } else {
                    resolvedArr.add(parts[dirVar]);
                }
            }
            System.out.println("Abs before construction: " + resolvedArr);
            pathString = absolutePathConstructor(resolvedArr);
        }
        // Relative path case
        else {
            System.out.println("RELATIVE");
            for (int dirVar = 0; dirVar < parts.length; dirVar++) {
                if (parts[dirVar].equals(".")) {
                    continue;
                } else if (parts[dirVar].equals("..")) {
                    if (!resolvedArr.isEmpty()) {
                        resolvedArr.remove(resolvedArr.size() - 1);
                    }
                } else {
                    resolvedArr.add(parts[dirVar]);
                }
            }
            System.out.println("Rel before construction: " + resolvedArr);
            pathString = relativePathConstructor(resolvedArr);
        }
        return pathString;
    }

    public String relativePathConstructor(ArrayList<String> relativeArr) { // Unfinished
        String fileString = "";
        if (relativeArr.size() != 0) {
            if (relativeArr.getFirst().equals(cwd))
                for (int resolvedDirs = 0; resolvedDirs < relativeArr.size(); resolvedDirs++) {
                    fileString += ("/" + relativeArr.get(resolvedDirs));
                }
            else if (!relativeArr.getFirst().equals(cwd)) {
                fileString += cwd;
                for (int resolvedDirs = 0; resolvedDirs < relativeArr.size(); resolvedDirs++) {
                    fileString += ("/" + relativeArr.get(resolvedDirs));
                }
            }
        }
        System.out.println("Relative string: " + fileString);
        System.out.println("\n");
        return fileString;
    }

    public String absolutePathConstructor(ArrayList<String> absoluteArr) {
        String fileString = "";
        if (absoluteArr.size() != 0) {
            if (!absoluteArr.getFirst().equals("home")) {
                for (int resolvedDirs = 0; resolvedDirs < absoluteArr.size(); resolvedDirs++) {
                    fileString += ("/" + absoluteArr.get(resolvedDirs));
                }
            } else if (absoluteArr.getFirst().equals("home")) {
                for (int resolvedDirs = 0; resolvedDirs < absoluteArr.size(); resolvedDirs++) {
                    fileString += ("/" + absoluteArr.get(resolvedDirs));
                }
            }
        }
        System.out.println("Absolute string: " + fileString);
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

