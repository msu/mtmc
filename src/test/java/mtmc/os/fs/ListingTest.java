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
        assertEquals(6, root.subdirectories.size());

        assertNotNull(root.subdirectories.get("bin"));
        assertNotNull(root.subdirectories.get("data"));
        assertNotNull(root.subdirectories.get("home"));
        assertNotNull(root.subdirectories.get("img"));
        assertNotNull(root.subdirectories.get("logs"));
        assertNotNull(root.subdirectories.get("src"));
    }
    
    @Test
    public void testCWD()
    {
        FileSystem fs = new FileSystem();
        Listing cwd = fs.listCWD();
        
        assertEquals("home", cwd.name);
        assertEquals("/home", cwd.path);
        assertEquals(1, cwd.subdirectories.size());

        assertNotNull(cwd.subdirectories.get("Justice"));
    }
    
    @Test
    public void testRelative()
    {
        FileSystem fs = new FileSystem();
        Listing cwd = fs.listFiles("../data");
        
        assertEquals("data", cwd.name);
        assertEquals("/data", cwd.path);
        assertEquals(2, cwd.listOfFiles.size());
        assertEquals(0, cwd.subdirectories.size());
    }
}
