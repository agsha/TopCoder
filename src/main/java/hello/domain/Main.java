package hello.domain;

import com.github.abrarsyed.jastyle.ASFormatter;
import com.github.abrarsyed.jastyle.FormatterHelper;
import com.github.abrarsyed.jastyle.constants.EnumFormatStyle;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import hello.crawler.TopcoderCrawler;
import hello.model.ProblemId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.*;

/**
 * Created by sharath on 4/28/15.
 */
public class Main {
    private static final Logger log = LogManager.getLogger();

    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");

    }
    public static Injector in = Guice.createInjector(new MyModule());


    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.go();
    }


    private void go() throws IOException {
        updateSchema();
        //createSchema();
        //fillAllProblemIds();
        //imageFiles();

    }

    private void imageFiles() throws IOException {
        List<Path> list = new ArrayList<>();

        Files.walkFileTree(Paths.get("/Users/sharath/projects/tcproblems"), ImmutableSet.of(), 1, new LambdaFileVisitor((path, blah) -> list.add(path)));
        for (Path path : list) {
            Path images = path.resolve("prob").resolve("images");
            if(!Files.isDirectory(images)) continue;
            Files.walkFileTree(images, new LambdaFileVisitor((p, blah) -> log.debug("{}", p.toString())));
        }
    }

    public void saveAllProblemsToFile(Path file) {
        TopcoderCrawler tcc = in.getInstance(TopcoderCrawler.class);
        tcc.authenticate();

        String response = tcc.doGet("http://community.topcoder.com/tc?module=ProblemArchive&sr=&er=4000&sc=&sd=&class=&cat=&div1l=&div2l=&mind1s=&mind2s=&maxd1s=&maxd2s=&wr=");
        try {
            Files.write(file, response.getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void fillAllProblemIds() {
        Session session = in.getInstance(SessionFactory.class).openSession();
        session.beginTransaction();
        for(int i=0; i<100; i++) {
            ProblemId pid = new ProblemId();
            session.save(pid);
        }
        session.getTransaction().commit();
     }

    private void createSchema() {
        SchemaExport exporter = in.getInstance(SchemaExport.class);
        exporter.execute(true, true, false, false);
    }

    private void updateSchema() {
        SchemaUpdate updater = in.getInstance(SchemaUpdate.class);
        updater.execute(true, true);
    }

    public static class LambdaFileVisitor extends SimpleFileVisitor<Path> {
        BiConsumer<Path, BasicFileAttributes> lambda;

        public LambdaFileVisitor(BiConsumer<Path, BasicFileAttributes> lambda) {
            this.lambda = lambda;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            lambda.accept(file, attrs);
            return FileVisitResult.CONTINUE;
        }
    }

    public static class DbJob<T> {
        Function<Session, T> work;

        public DbJob(Function<Session, T> work) {
            this.work = work;
        }

        public T doWork() {
            Session session = Main.in.getInstance(SessionFactory.class).openSession();
            session.beginTransaction();
            try {
                T ret = work.apply(session);
                session.getTransaction().commit();
                return ret;
            } catch (Exception e) {
                session.getTransaction().rollback();
                throw e;
            } finally {
                session.close();
            }
        }
    }

    public static String l(Object ...o) {
        String s = "";
        if(o == null) return "null";
        for(Object oo : o) {
            if(oo instanceof int[]) {
                s+= Arrays.toString((int[]) oo)+" ";
                continue;
            }
            if(oo instanceof double[]) {
                s+=Arrays.toString((double[])oo)+" ";
                continue;
            }
            if(oo instanceof String[]) {
                s+=Arrays.toString((String[]) oo)+" ";
                continue;
            }
            if(oo instanceof int[][]) {
                s+= Arrays.deepToString((int[][]) oo)+" ";
                continue;
            }
            if(oo instanceof double[][]) {
                s+= Arrays.deepToString((double[][])oo)+" ";
                continue;
            }
            if(oo instanceof String[][]) {
                s+= Arrays.deepToString((String[][])oo)+" ";
                continue;
            }
            if(oo instanceof int[][][]) {
                s+= Arrays.deepToString((int[][][])oo)+" ";
                continue;
            }
            if(oo instanceof double[][][]) {
                s+= Arrays.deepToString((double[][][])oo)+" ";
                continue;
            }
            if(oo instanceof String[][][]) {
                s+= Arrays.deepToString((String[][][]) oo)+" ";
                continue;
            }
            if(oo instanceof long[]) {
                s+= Arrays.toString((long[]) oo)+" ";
                continue;
            }
            if(oo instanceof long[][]) {
                s+= Arrays.deepToString((long[][]) oo)+" ";
                continue;
            }
            if(oo instanceof long[][][]) {
                s+= Arrays.deepToString((long[][][])oo)+" ";
                continue;
            }
            if(oo instanceof Object[]) {
                s+= Arrays.deepToString((Object[])oo)+" ";
            }
            if(oo == null) {
                s += "null ";
                continue;
            }
            s += (oo.toString())+" ";
        }
        return s;
    }

    public static String formatJavaCode(String code) throws Exception {
        ASFormatter formatter = new ASFormatter();

        // bug on lib's implementation. reported here: http://barenka.blogspot.com/2009/10/source-code-formatter-library-for-java.html
//        code.replace("{", "{\n");
//
//        Reader in = new BufferedReader(new StringReader(code));
//        formatter.setFormattingStyle(EnumFormatStyle.JAVA);
//        return FormatterHelper.format(in, formatter);
        return code;
    }
}
