package hello.crawler;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by sharath on 5/2/15.
 */
public interface ParameterReader {
    public Object next(Type type) throws IOException;

    void next() throws IOException;
}
