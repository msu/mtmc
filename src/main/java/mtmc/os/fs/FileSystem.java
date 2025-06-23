package mtmc.os.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
        if (fileString.equals("")) {
            fileString = "/";
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

    private File toRealPath(String path) { // Resolves given path and returns /disk/ + path
        String resolvedPath = resolve(path);
        String slashGone = resolvedPath.substring(1);
        return DISK_PATH.resolve(slashGone).toFile();
    }

    public Listing listFiles(String path) {

        File resolvedPath = toRealPath(path);
        Listing listing = listFilesRecursive(resolvedPath, 0);
        return listing;
    }

    private Listing listFilesRecursive(File filePath, int depth) {
        Listing listing = new Listing(filePath, new ArrayList<>(), new LinkedHashMap<>()); // Set Listing() path to current level
        if (!filePath.isDirectory()) {
            // This is never entered
            return null;
        }
        File[] files = filePath.listFiles();
        for (int i = 0; i < files.length; i++) {

            File f = files[i];
            if (f.isDirectory()) { // Detects whether to print String Dir or File
                listing.subdirectories.put(f.getName(), new Listing(f, new ArrayList<>(), new LinkedHashMap<>()));
                // Maybe works. (Testing object scope)
                System.out.println(("\t").repeat(depth) + f.getName());
                listFilesRecursive(f, depth + 1);


            } else {
                listing.listOfFiles.add(f);
                System.out.println(("\t").repeat(depth) + f.getName());
            }
        }
        return listing;
    }

    public void writeFile(String path, String contents) throws IOException {
        Path filePath = getDiskPath(path);
        Files.writeString(filePath, contents);
    }

    public String readFile(String path) throws FileNotFoundException, IOException {
        Path filePath = getDiskPath(path);
        var contents = Files.readString(filePath);
        return contents;
    }
}