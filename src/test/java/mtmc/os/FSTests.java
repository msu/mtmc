package mtmc.os;

import mtmc.os.fs.FileSystem;
import mtmc.os.fs.Listing;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FSTests {
    @Test
    public void testPaths() {
        var fs = new FileSystem();

        assertAll(
                () -> assertEquals("/bin/bin/hello_world", fs. resolve("/bin/./bin/hello_world")),
                () -> assertEquals("/bin/hello_world", fs.resolve("../bin/hello_world")),
                () -> assertEquals("/bin/hello_world", fs.resolve("../../bin/hello_world")),
                () -> assertEquals("/home/hello_world", fs.resolve("hello_world")),
                () -> assertEquals("/home/hello_world", fs.resolve("/home/./hello_world")),
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

    @Test
    public void testPathJoin() {
        var fs = new FileSystem();
        assertAll(
                () -> assertEquals("/bin/pwd", fs.join("/bin", "pwd")),
                () -> assertEquals("/bin/./echo", fs.join("/bin", "./echo")),
                () -> assertEquals("/bin/../../echo", fs.join("/bin", "../../echo")),
                () -> assertEquals("scoobie/doobie", fs.join("scoobie", "doobie")),
                () -> {
                    fs.setCWD("/img");
                    assertEquals("/img/dog", fs.join("/img", "dog"));
                }
        );
    }


    @Test
    public void testListFiles() {
        var fs = new FileSystem();
        //fs.listFiles("../bin");
        fs.listFiles("..");
        //fs.listFiles("../home");

    }
}
