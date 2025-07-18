package mtmc.os.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import mtmc.os.exec.Executable;

/**
 *
 * @author jbanes
 */
public class ExecutableFileTypeDetector extends FileTypeDetector {

    @Override
    public String probeContentType(Path path) throws IOException {
        try(var in = Files.newInputStream(path)) {
            if(in.read() != '{') return null;
        }
        
        try {
            Executable.load(path);
            
            return "text/mtmc16-bin";
        } catch(IOException e) {
            return null;
        }
        
    }
    
}
