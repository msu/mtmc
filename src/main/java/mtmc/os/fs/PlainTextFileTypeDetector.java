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
public class PlainTextFileTypeDetector extends FileTypeDetector {

    @Override
    public String probeContentType(Path path) throws IOException {
        int c;
        
        try(var in = Files.newInputStream(path)) {
            // Detect non-ASCII chararcters
            while ((c = in.read()) >= 0) {
                if (c < 9 || c > 126) {
                    return null;
                }
            }
        }
        
        return "text/plain";
        
    }
    
}
