package hello.crawler;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

import static hello.domain.Main.l;

/**
 * Created by sharath on 5/5/15.
 */
public class SharathDemoReader extends SharathSysReader {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        SharathDemoReader reader = new SharathDemoReader(Files.newBufferedReader(Paths.get("/Users/sharath/projects/tcproblems/Aaagmnrs/data/demo/0.in")));
        Scanner sc = new Scanner(Files.newBufferedReader(Paths.get("/Users/sharath/projects/tcproblems/Aaagmnrs/data/demo/0.in")));
        sc.useDelimiter("\\Z");
        log.debug("{}", sc.next());
    }
    public SharathDemoReader(Reader reader) {
        super(reader);
    }

    @Override
    public int[] nextIntArray() {
        return getInts(getArrayString());
    }

    @Override
    public long[] nextLongArray() {
        return getLongs(getArrayString());
    }

    @Override
    public double[] nextDoubleArray() {
        return getDoubles(getArrayString());
    }

    protected String getArrayString() {
        String str = "";
        /*while(true) {
            if(!sc.hasNextLine()) {
                log.debug("{}, fooo", str);
                sc.useDelimiter("\\Z");
                String ss = sc.next();
                if(ss.charAt(ss.length()-1)!=']') {
                    throw new RuntimeException("Last character must be ']");
                }
                str+=ss;
                break;
            }


            String ss = sc.nextLine();
            log.debug("{} whhhaa", ss);
            str+=ss;
            if(ss.endsWith("],")) {
                break;
            }
        }*/
        while(true) {
            String ss = sc.nextLine();
            ss = ss.trim();
            str+=ss;
            if(sc.hasNextLine()&&ss.endsWith("],") || !sc.hasNextLine()&&ss.charAt(ss.length()-1)==']') {
                break;
            }
        }
        return str;
    }

    @Override
    public String[] nextStringArray() {
        String arrayString = getArrayString();
        String[] strings = getStrings(arrayString, "\" ?, *\"");
        log.debug(l(arrayString, strings, strings.length));
        return strings;
    }

    @Override
    public char nextChar() {
        String str = sc.nextLine().trim();
        log.debug(l("next line", str));
        if(str.charAt(0)=='\'') str = str.substring(1);
        return str.charAt(0);

    }
}
