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


    public void setCWD(String cwd) {
        this.cwd = resolve(cwd);
    }

    public String getCWD() {
        return cwd;
    }

    public String resolve(String fileName) {
        String resolvedString = "";
        String[] cwdPath = cwd.split("/");
        ArrayList<String> cwdArrayList = new ArrayList<>(Arrays.asList(cwdPath)); // Convert to ArrayList
//        if (cwdPath[0].equals("")) { // Remove empty string (First slash)
//            cwdArrayList.removeFirst();
//        }
        String[] path = fileName.split("/");
        ArrayList<String> fileArrayList = new ArrayList<>(Arrays.asList(path)); // Convert to ArrayList
        resolvedString = pathConstructor(cwdArrayList, fileArrayList); // Handles absolute and relative construction

        return resolvedString;
    }
    /*public String pathConstructor(ArrayList<String> cwd, ArrayList<String> path){
        ArrayList<String> constructedPath = new ArrayList<>();
        if(path.get(0).equals("")){
            path.removeFirst();
        }
    }*/
    public String pathConstructor(ArrayList<String> cwd, ArrayList<String> path) {
        ArrayList<String> constructedPath = new ArrayList<>();
        if (path.isEmpty()) { // This fulfills (cd " ")
            // do nothing, just a guard for path.get(0)
        } else if (path.get(0).equals("")) {
            path.removeFirst();
            for (String link : path) {
                if(link.equals("..") && constructedPath.isEmpty()){
                }
                else if (link.equals("..") && !constructedPath.isEmpty()) { // Check if there is a directory to move up
                    constructedPath.removeLast();
                } else if (!link.equals(".")) { // Don't add "." to path string
                    constructedPath.add(link);
                }
            }
        } else { // Else-If given path is absolute
            constructedPath.addAll(cwd); // Only added when ".." or "." are at the beginning
            for (String link : path) {
                if(link.equals("..") && constructedPath.isEmpty()){}
                if (link.equals("..") && !constructedPath.isEmpty()) { // Check if there is a directory to move up
                    constructedPath.removeLast();
                } else if (!link.equals(".")) { // Don't add "." to path string
                    constructedPath.add(link);
                }
            }
        }

        if (constructedPath.isEmpty()) return "/";
        if (constructedPath.get(0).isEmpty()) {
            constructedPath.removeFirst();
        }
        String fileString = "";
        for (String link : constructedPath) {
            fileString += ("/" + link);
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

    public File getRealPath(String path) { // Resolves given path and returns /disk/ + path
        String resolvedPath = resolve(path);
        String slashGone = resolvedPath.substring(1);
        return DISK_PATH.resolve(slashGone).toFile();
    }

    public Listing listFiles(String path) {
        File resolvedPath = getRealPath(path);
        Listing listing = listFilesRecursive(resolvedPath, 0);
        return listing;
    }

    private Listing listFilesRecursive(File file, int depth) {
        String mtmcPath = file.getPath().substring(DISK_PATH.toAbsolutePath().toString().length());
        Listing listing = new Listing(mtmcPath, file.getName(), new ArrayList<>(), new LinkedHashMap<>()); // Set Listing() path to current level
        if (!file.isDirectory()) {
            // This is never entered
            return null;
        }
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {

            File f = files[i];
            if (f.isDirectory()) {
                //listing.subdirectories.put(f.getName(), new Listing(f, new ArrayList<>(), new LinkedHashMap<>())); Wrong
                //System.out.println(("\t").repeat(depth) + f.getName());
                listing.subdirectories.put(f.getName(), listFilesRecursive(f, depth + 1));
            } else {
                listing.listOfFiles.add(f);
                //System.out.println(("\t").repeat(depth) + f.getName());
            }
        }
        return listing;
    }

    public Listing listCWD() {
        return listFiles(cwd);
    }

    public Listing listRoot() {
        return listFiles("/");
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