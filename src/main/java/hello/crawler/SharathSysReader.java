package hello.crawler;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static hello.domain.Main.l;

/**
 * Created by sharath on 5/2/15.
 */
public class SharathSysReader {
    private static final Logger log = LogManager.getLogger();
    public static final String ARRAY_DELIMITER = "\\[|,\\s*|\\s*\\]";

    public static void main(String[] args) throws IOException {
        BufferedReader bf = Files.newBufferedReader(Paths.get("/Users/sharath/projects/tcproblems/AutoAdjust/data/sys/21.in"));
        SharathSysReader s = new SharathSysReader(bf);
        log.debug("{}", Arrays.toString(s.nextStringArray()));
    }

    protected Reader reader;
    protected Scanner sc;

    public SharathSysReader(Reader reader) {
        this.reader = new BufferedReader(reader, 1<<16);
        sc = new Scanner(reader);
    }

    public int[] nextIntArray() {
        String str = sc.nextLine();
        return getInts(str);
    }

    protected int[] getInts(String str) {
        String regex = "[^\\d+\\-]+";
        String[] strings = Iterables.toArray(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str), String.class);
        int[] ret = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            ret[i] = Integer.parseInt(s);
        }
        return ret;
    }

    public double[] nextDoubleArray() {
        String str = sc.nextLine();
        return getDoubles(str);
    }

    protected double[] getDoubles(String str) {
        String regex = "[^\\d+\\-\\.eE]+";
        String[] strings = Iterables.toArray(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str), String.class);
        double[] ret = new double[strings.length];
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            ret[i] = Double.parseDouble(s);
        }
        return ret;
    }

    public boolean nextBoolean() {
        String str = sc.nextLine();
        String regex = ",";
        return Boolean.parseBoolean(Iterables.getOnlyElement(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str)));
    }


    public double nextDouble() {
        String str = sc.nextLine();
        String regex = ",";
        return Double.parseDouble(Iterables.getOnlyElement(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str)));
    }


    public char nextChar() {
        String str = sc.nextLine();
        return str.charAt(0);
    }

    public String nextString() {
        String str = sc.nextLine();
        str = str.substring(str.indexOf('"')+1, str.lastIndexOf('"'));
        str = str.replace("\\\\", "\\");
        return str;
    }

    public int nextInt(){
        String str = sc.nextLine();
        log.debug("next int line is:{}", str);
        String regex = ",";
        return Integer.parseInt(Iterables.getOnlyElement(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str)));
    }


    public long nextLong(){
        String str = sc.nextLine();
        String regex = ",";
        return Long.parseLong(Iterables.getOnlyElement(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str)));
    }


    public long[] nextLongArray() {
        String str = sc.nextLine();
        return getLongs(str);
    }

    protected long[] getLongs(String str) {
        String regex = "[^\\d+\\-]+";
        String[] strings = Iterables.toArray(Splitter.on(Pattern.compile(regex)).omitEmptyStrings().trimResults().split(str), String.class);
        long[] ret = new long[strings.length];
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            ret[i] = Long.parseLong(s);
        }
        return ret;
    }

    public String[] nextStringArray() {
        String str = sc.nextLine();
        return getStrings(str, "\", \"");
    }

    protected String[] getStrings(String str, String delimiter) {
        //log.debug("foo {}", str);
        if(str.charAt(0)!='[') {
            throw new RuntimeException("expected '[' for nextStringArray, but found "+ str.charAt(0));
        }
        str = str.substring(1);
        if(str.charAt(str.length()-1)==',') {
            str = str.substring(0, str.length()-1);
        }
        if(str.charAt(str.length() - 1)!=']') {
            throw new RuntimeException("expected ']' for nextStringArray, but found "+str.charAt(str.length()-1));
        }
        str = str.substring(0, str.length() - 1);
        str = str.trim();

        if(str.length()==0) {
            return new String[0];
        }
        if(str.charAt(0)!='"') {
            throw new RuntimeException("expected '\"' for "+str+" first character, but found "+str.charAt(0));
        }
        str = str.substring(1);
        if(str.charAt(str.length()-1) != '"') {
            throw new RuntimeException("expected '\"' for "+str+"  last character, but found "+str.charAt(str.length()-1));
        }
        str = str.substring(0, str.length() - 1);
        Splitter.on(Pattern.compile(delimiter)).split(str);
        String[] split = Iterables.toArray(Splitter.on(Pattern.compile(delimiter)).split(str), String.class);
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].replace("\\\\", "\\");

        }
        return split;
    }

    private void expect(char c) throws Exception {
        int read = reader.read();
        if(read!=c) throw new Exception("expected "+c+"  found "+read);

    }

}
