package mtmc.util;

import java.io.Closeable;

public interface SafeClosable extends Closeable {
    @Override
    void close();
}
