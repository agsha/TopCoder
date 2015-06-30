package hello.crawler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import static hello.domain.Main.l;

/**
 * Created by sharath on 5/5/15.
 */
public class SharathDemoReturnReader extends SharathDemoReader {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
    }
    public SharathDemoReturnReader(Reader reader) {
        super(reader);
    }

    @Override
    public String[] nextStringArray() {
        String arrayString = getArrayString();
        String[] strings = getStrings(arrayString, "\", ? ?\"");
        log.debug(l(arrayString, strings, strings.length));
        return strings;
    }
}
