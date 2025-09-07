package mtmc.os.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
        
        initFileSystem();
    }

    private void initFileSystem() {
        if (DISK_PATH.toFile().exists()) return;
        
        // Make the disk/ directory
        DISK_PATH.toFile().mkdirs();
        
        try (var in = new ZipInputStream(getClass().getResourceAsStream("/disk.zip"))) {
            ZipEntry entry;
            File file;
            
            byte[] data = new byte[4096];
            int count;

            while ((entry = in.getNextEntry()) != null) {
                file = new File(entry.getName());
                
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    
                    try (var out = new FileOutputStream(file)) {
                        while((count = in.read(data)) > 0) {
                            out.write(data, 0, count);
                        }
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private void notifyOfFileSystemUpdate() {
        if (this.computer != null) {
            computer.notifyOfFileSystemUpdate();
        }
    }
    
    public String getCWD() {
        return this.cwd;
    }
    
    public void setCWD(String cwd) {
        this.cwd = resolve(cwd);
        this.notifyOfFileSystemUpdate();
    }
    
    public boolean exists(String path) {
        return new File(DISK_PATH.toFile(), resolve(path)).exists();
    }
    
    public boolean mkdir(String path) {
        boolean success = new File(DISK_PATH.toFile(), resolve(path)).mkdir();
        
        if (success) {
            computer.notifyOfFileSystemUpdate();
        }
        
        return success;
    }
    
    public boolean delete(String path) {
        boolean success = new File(DISK_PATH.toFile(), resolve(path)).delete();
        
        if (success) {
            computer.notifyOfFileSystemUpdate();
        }
        
        return success;
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
    
    public String getMimeType(String path) throws IOException {
        var file = getRealPath(path);
        var name = file.toFile().getName().toLowerCase();
        var probed = Files.probeContentType(file);
        
        if(name.endsWith(".asm")) return "text/x-asm";
        if(name.endsWith(".c")) return "text/x-csrc";
        if(name.endsWith(".sea")) return "text/x-csrc";
        if(name.endsWith(".h")) return "text/x-csrc";
        if(probed != null) return probed;
        
        return "application/octet-stream";
    }
    
    public InputStream openFile(String path) throws IOException {
        var file = getRealPath(path).toFile();
        
        return new FileInputStream(file);
    }
    
    public void saveFile(String path, InputStream contents) throws IOException {
        var file = getRealPath(path).toFile();
        byte[] data = new byte[4096];
        int count;
        
        try(var out = new FileOutputStream(file)) {
            while((count = contents.read(data)) > 0) {
                out.write(data, 0, count);
            }
        }
    }
}