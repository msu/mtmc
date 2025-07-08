package mtmc.os.fs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import mtmc.emulator.MonTanaMiniComputer;

public class FileSystem {
    private String cwd = "/home";
    private MonTanaMiniComputer computer;
    static final Path DISK_PATH = Path.of(System.getProperty("user.dir"), "disk").toAbsolutePath();
    
    public FileSystem() {
        this(null);
    }
    
    public FileSystem(MonTanaMiniComputer computer) {
        this.computer = computer;
    }

    private void notifyOfFileSystemUpdate() {
        if (this.computer != null) {
            computer.notifyOfFileSystemUpdate();
        }
    }
    
    public void setCWD(String cwd) {
        this.cwd = resolve(cwd);
        this.notifyOfFileSystemUpdate();
    }
    
    public boolean exists(String path) {
        return new File(DISK_PATH.toFile(), resolve(path)).exists();
    }

    public String resolve(String filename) {
        File root = DISK_PATH.toFile();
        File directory = filename.startsWith("/") ? root : new File(root, cwd);
        String[] path = filename.split("/");
        
        for (String name : path) {
            if (name.equals(".")) {
                continue;
            } else if (name.equals("..") && directory.equals(root)) {
                continue;
            } else if (name.equals("..")) {
                directory = directory.getParentFile();
            } else {
                directory = new File(directory, name);
            }
        }
        
        if(directory.equals(root)) {
            return "/";
        }

        return directory.getAbsolutePath().substring(root.getAbsolutePath().length());
    }

    public Path getRealPath(String path) { // Resolves given path and returns /disk/ + path
        String resolvedPath = resolve(path);
        String slashGone = resolvedPath.length() > 0 ? resolvedPath.substring(1) : "";
        File file = DISK_PATH.resolve(slashGone).toFile();
        
        if (file.getAbsolutePath().length() < DISK_PATH.toFile().getAbsolutePath().length()) {
            return DISK_PATH;
        }
        
        return file.toPath();
    }
    
    public File[] getFileList(String path) {
        File resolvedPath = getRealPath(path).toFile();
        
        if (!resolvedPath.isDirectory()) return new File[]{ resolvedPath };
        
        return resolvedPath.listFiles();
    }

    public Listing listFiles(String path) {
        File resolvedPath = getRealPath(path).toFile();
        
        return new Listing(this, resolvedPath);
    }

    public Listing listCWD() {
        return listFiles(cwd);
    }

    public Listing listRoot() {
        return listFiles("/");
    }

    public void writeFile(String path, String contents) throws IOException {
        Path filePath = getRealPath(path);
        Files.writeString(filePath, contents);
    }

    public String readFile(String path) throws FileNotFoundException, IOException {
        Path filePath = getRealPath(path);
        var contents = Files.readString(filePath);
        return contents;
    }
}