package hello.crawler;

import hello.domain.Main;
import hello.model.Problem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sharath on 5/20/15.
 */
public class TcproblemsParser {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        TcproblemsParser tcp = new TcproblemsParser();
        try {
            tcp.go();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    private void go() throws IOException {
        Map<String, String> classNameToSignatureMap = new HashMap<>();
        Pattern p = Pattern.compile("- \\*Method signature\\*: `(.*)`");
        int[] count=new int[1];
        Files.walkFileTree(Paths.get("/Users/sharath/projects/tcproblems"), new Main.LambdaFileVisitor((path, blah) -> {
            Path fileName = path.getFileName();
            if(fileName.toString().endsWith(".md")) {
                try {
                    String contents = new String(Files.readAllBytes(path));
                    Matcher matcher = p.matcher(contents);
                    if(!matcher.find()) {
                        throw new RuntimeException("Could not find signature for "+path.toString());
                    }
                    String signature = matcher.group(1);

                    String className = fileName.toString().substring(0, fileName.toString().length() - 3);
                    classNameToSignatureMap.put(className, signature);
                    log.debug("#{}:{}, {}", count[0]++, className, signature);
                    //log.debug("{}, {}", signature);
                    new Main.DbJob<Void>(session -> {
                        Problem problem = Problem.fromName(session, className);
                        if(problem==null) log.debug("noooo{}", className);
                        problem.signature = signature;
                        session.update(problem);
                        return null;
                    }).doWork();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }
}
