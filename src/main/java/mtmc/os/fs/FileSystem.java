package mtmc.os.fs;

import org.eclipse.jetty.util.ArrayUtil;

import java.io.File;
import java.nio.file.Path;
import java.sql.Array;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class FileSystem {
    private static Map<String, ArrayList<String>> DIRECTORY_W_FILES = new TreeMap<String, ArrayList<String>>();
    private String cwd = "/home";
    static final Path DISK_PATH = Path.of(System.getProperty("user.dir"), "disk").toAbsolutePath();
    static final Path HOME_PATH = Path.of(DISK_PATH.toString(), "/home");


    public void setCWD(String cd) {
        cwd = cd;
    }


    public String resolve(String fileName) {
        String resolvedString = "";
        String[] cwdPath = cwd.split("/");
        ArrayList<String> cwdArrayList = new ArrayList<>(Arrays.asList(cwdPath)); // Convert to ArrayList
        if (cwdPath[0].equals("")) { // Remove empty string (First slash)
            cwdArrayList.removeFirst();
        }
        String[] path = fileName.split("/");
        ArrayList<String> fileArrayList = new ArrayList<>(Arrays.asList(path)); // Convert to ArrayList
        resolvedString = pathConstructor(cwdArrayList, fileArrayList); // Handles absolute and relative construction

        return resolvedString;
    }

    public String pathConstructor(ArrayList<String> cwd, ArrayList<String> path) {
        String fileString = "";
        System.out.println("Path: " + path.toString());
        ArrayList<String> constructedPath = new ArrayList<>();
        if (path.isEmpty()) { // This fulfills (cd " ")
            for (int cwdDir = 0; cwdDir < cwd.size(); cwdDir++) {
                fileString += ("/" + cwd.get(cwdDir));
            }
        } else if (path.get(0).equals("") || path.get(0).equals("..") || path.get(0).equals(".")) { // Else-If given path is absolute
            if (path.get(0).equals("")) { // Delete slash
                path.removeFirst();
            }
            if (path.get(0).equals("..") || path.get(0).equals(".")) { // Add the cwd only if it starts with "." or ".."
                constructedPath.addAll(cwd); // Only added when ".." or "." are at the beginning
            }
            for (int i = 0; i < path.size(); i++) {
                if (path.get(i).equals("..") && !constructedPath.isEmpty()) { // Check if there is a directory to move up
                    constructedPath.removeLast();
                } else if (path.get(i).equals("..") && constructedPath.isEmpty()) { // Do nothing if no upper directory
                    continue;
                } else if (path.get(i).equals(".")) { // Don't add "." to path string
                    continue;
                } else {
                    constructedPath.add(path.get(i));
                }
            }
            for (int i = 0; i < constructedPath.size(); i++) { // Construct file string for absolute path
                fileString += ("/" + constructedPath.get(i));
            }

        } else { // Relative handling
            constructedPath.addAll(cwd); // Add cwd to empty path
            constructedPath.addAll(path); // Add path to cwd because relative
            for (int i = 0; i < constructedPath.size(); i++) { // Construct file string with "/"
                fileString += ("/" + constructedPath.get(i));
            }
        }
        return fileString;
    }

    public String join(String front, String back) {
        String joinedPath = "";
        String[] parts = back.split("/");
        if (!parts[0].equals("/")) {
            joinedPath = front + "/" + back;
        } else if (parts[0].equals("/")) {
            joinedPath = front + back;
        }
        return joinedPath;
    }

    public void listFiles(String path) {
        /*File resolvedPath = toRealPath(path);
        File[] files = resolvedPath.listFiles();
        System.out.println(Arrays.toString(files));
        if(files == null){
            System.out.println("File path does not exist or has no children.");
            return;
        }
        for (File file : files) {
            // If a subdirectory is found,
            // print the name of the subdirectory

            if (file.isDirectory()) {
                System.out.println("Directory: " + file.getName());
                *//*for (int i = -1; i < files[file].length; i++) {

                }*//*
                System.out.println("test: " + Arrays.toString(file.listFiles()));
            }
            else {
                // Print the file name
                System.out.println("File: " + file.getName());
            }
        }
        return;*/
    }

}
