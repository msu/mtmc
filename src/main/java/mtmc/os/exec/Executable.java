package mtmc.os.exec;

import com.google.gson.Gson;
import mtmc.emulator.DebugInfo;

import java.io.*;
import java.nio.file.Path;

public record Executable(
        Format format,
        byte[] code,
        byte[] data,
        byte[][] graphics,
        String sourceName,
        DebugInfo debugInfo) {
    public enum Format {
        Orc1("orc1");

        public final String name;

        Format(String name) {
            this.name = name;
        }
    }

    public String dump() {
        return new Gson().toJson(this);
    }

    public void dump(Path path) throws IOException {
        try (var fw = new FileWriter(path.toFile())) {
            dump(fw);
        }
    }

    public void dump(Writer writer) {
        var gson = new Gson();
        gson.toJson(this, writer);
    }

    public static Executable load(String exe) {
        return new Gson().fromJson(exe, Executable.class);
    }

    public static Executable load(Path path) throws IOException {
        try (var fw = new FileReader(path.toFile())) {
            return load(fw);
        }
    }

    public static Executable load(Reader reader) throws IOException {
        var gson = new Gson();
        return gson.fromJson(reader, Executable.class);
    }
}
