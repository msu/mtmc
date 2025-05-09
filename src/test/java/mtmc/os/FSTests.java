package mtmc.os;

import mtmc.os.fs.FileSystem;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FSTests {
    @Test
    public void testPaths() {
        var fs = new FileSystem();

        assertAll(
            () -> assertEquals("/bin/hello_world", fs.resolve("/bin/../bin/hello_world")),
            () -> assertEquals("/home/hello_world", fs.resolve("hello_world")),
            () -> assertEquals("/home/hello_world", fs.resolve("/home/./hello_world"))
        );
    }

    @Test
    public void testListFiles() {
        var fs = new FileSystem();
        fs.listFiles("/bin");
    }
}
