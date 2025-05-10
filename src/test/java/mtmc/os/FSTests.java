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
            () -> assertEquals("/home/hello_world", fs.resolve("/home/./hello_world")),
            () -> {
                fs.setPWD("/bin");
                assertEquals("/bin/cat", fs.resolve("./cat"));
                assertEquals("/bin/cat", fs.resolve("cat"));
            }
        );
    }

    @Test
    public void testPathJoin() {
        var fs = new FileSystem();
        assertAll(
            () -> assertEquals("/bin/pwd", fs.join("/bin", "cat")),
            () -> assertEquals("/bin/echo", fs.join("/bin", "./echo"))
        );
    }

    @Test
    public void testListFiles() {
        var fs = new FileSystem();
        fs.listFiles("/bin");
    }
}
