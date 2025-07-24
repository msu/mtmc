package mtmc.os.fs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author jbanes
 */
public class ListingTest
{
    @Test
    public void testRoot()
    {
        FileSystem fs = new FileSystem();
        Listing root = fs.listRoot();
        
        assertEquals("/", root.name);
        assertEquals("/", root.path);
        assertTrue(root.directory);
        
        assertEquals("bin", root.list().get(0).name);
        assertEquals("data", root.list().get(1).name);
        assertEquals("home", root.list().get(2).name);
        assertEquals("img", root.list().get(3).name);
        assertEquals("logs", root.list().get(4).name);
        assertEquals("src", root.list().get(5).name);
    }
    
    @Test
    public void testCWD()
    {
        FileSystem fs = new FileSystem();
        Listing cwd = fs.listCWD();
        
        assertEquals("home", cwd.name);
        assertEquals("/home", cwd.path);
    }
    
    @Test
    public void testRelative()
    {
        FileSystem fs = new FileSystem();
        Listing cwd = fs.listFiles("../img");
        
        assertEquals("img", cwd.name);
        assertEquals("/img", cwd.path);
        assertEquals(4, cwd.list().size());
    }
}
