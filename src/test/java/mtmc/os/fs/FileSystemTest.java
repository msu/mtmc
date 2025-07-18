package mtmc.os.fs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTest {
    @Test
    public void testPaths() {
        var fs = new FileSystem();

        assertAll(
                () -> assertEquals("/", fs.resolve("/")),
                () -> assertEquals("/bin/bin/hello_world", fs. resolve("/bin/./bin/hello_world")),
                () -> assertEquals("/bin/hello_world", fs.resolve("../bin/hello_world")),
                () -> assertEquals("/bin/hello_world", fs.resolve("../../bin/hello_world")),
                () -> assertEquals("/home/hello_world", fs.resolve("hello_world")),
                () -> assertEquals("/home/hello_world", fs.resolve("/home/./hello_world")),
                () -> {
                    fs.setCWD("/home/user/dillon/projects/testcase");
                    assertEquals("/home/user/dillon/projects/.local/bin", fs.resolve("hello/../../.local/bin"));
                },
                () -> {
                    fs.setCWD("/home/user/dillon/projects/testcase");
                    assertEquals("/home/user/dillon/.local/bin", fs.resolve("../../.local/bin"));
                },
                () -> {
                    fs.setCWD("/bin");
                    assertEquals("/bin/cat", fs.resolve("./cat"));
                    assertEquals("/bin/cat", fs.resolve("cat"));
                }
        );
    }
}
