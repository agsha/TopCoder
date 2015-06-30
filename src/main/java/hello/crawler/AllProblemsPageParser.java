package hello.crawler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import hello.domain.Main;
import hello.model.Problem;
import hello.model.ProblemId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

/**
 * Created by sharath on 5/7/15.
 */
public class AllProblemsPageParser {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        AllProblemsPageParser page = new AllProblemsPageParser(new String(Files.readAllBytes(Paths.get("allProblemsPage"))));
        try {
            page.go();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    private void go() throws IOException {
        saveProblemsInDb();
    }

    private void saveProblemsInDb() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat formatter = new SimpleDateFormat("MM.dd.yyyy");
        List<ProblemId> list = mapper.readValue(Paths.get("hello.crawler.AllProblemsPageParser.problemIds").toFile(), new TypeReference<List<ProblemId>>() {
        });
        Set<Integer> duplicates = new HashSet<>();
        int index = 0;
//        for (ProblemId problemId : list) {
//            problemId.index = index;
//            if(!ids.contains(problemId.problemId)) {
//                ids.add(problemId.problemId);
//            } else {
//                duplicates.add(problemId);
//                //log.debug("{}", problemId);
//            }
//            index++;
//        }
        log.debug("{}", duplicates.size());

        new Main.DbJob<Void>(session -> {
            for (ProblemId problemId : list) {
                Problem problem = new Problem();
                problem.problemId = problemId.problemId;
                problem.className = problemId.className;
                problem.matchId = problemId.matchId;
                try {
                    problem.date = formatter.parse(problemId.date);
                } catch (ParseException e) {
                    throw  new RuntimeException(e);
                }
                if(!duplicates.contains(problemId.problemId)) {
                    session.saveOrUpdate(problem);
                    duplicates.add(problemId.problemId);
                }
            }
            return null;
        }).doWork();
    }
    private List<ProblemId> parse() {
        List<ProblemId> problemIds = new ArrayList<>();
        Document doc = Jsoup.parse(allProblemsPage);
        Elements trs = doc.select("table.paddingTable2").get(1).child(0).children();
        for (int i = 3; i < trs.size()-6; i++) {
            Element tr = trs.get(i);
            Element a = tr.child(1).child(0);
            String detailsHref = "http://community.topcoder.com"+tr.child(10).child(0).attr("href");
            int pid = getPid(a.attr("href"));
            String className = a.text();
            String date = tr.child(3).text();
            Element matchA = tr.child(2).child(0);
            int matchId = getMatchId(matchA.attr("href"));
            problemIds.add(new ProblemId(className, date, pid, detailsHref, matchId));
            log.debug("{}, {}, {}, {}, {}", className, date, matchId, detailsHref, pid);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(Paths.get("hello.crawler.AllProblemsPageParser.problemIds").toFile(), problemIds );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return ImmutableList.of();

    }

    private int getMatchId(String href) {
        int ind = href.lastIndexOf('=')+1;
        return Integer.parseInt(href.substring(ind));
    }

    private int getPid(String href) {
        int ind = href.lastIndexOf('=')+1;
        return Integer.parseInt(href.substring(ind));
    }

    public AllProblemsPageParser(String allProblemsPage) {
        this.allProblemsPage = allProblemsPage;
    }

    private String allProblemsPage;
}
