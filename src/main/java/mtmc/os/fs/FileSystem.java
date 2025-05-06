package mtmc.os.fs;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import io.javalin.http.Context;
import mtmc.web.WebServer;

public class FileSystem {
    private static Map<String, ArrayList<String>> DIRECTORY_W_FILES = new TreeMap<String, ArrayList<String>>();
    Context fileTestVar = WebServer.ctx;

    /*
    TODO: Create "directories" variable to iterate
    for (directory = 0; directory < directories.length; directory++) // Iterate through directories
        if (directory.isDirectory){
            return;
        }
        else if(!directory.isDirectory){ // If directory doesn't exist, add to TreeMap. Then iterate and add files within
            // TODO:
                - Create array of files within directory
                - Input array(files[directory.length] into TreeMap
                - TreeMap <directory, files>

            DIRECTORY_W_FILES.put(directory, TODO:ARRAY_VAR);
        } throws FileNotFoundException("File not found");

        public static boolean isDirectory(String directory) {return DIRECTORIES.containsKey(directory);}

     */
}

