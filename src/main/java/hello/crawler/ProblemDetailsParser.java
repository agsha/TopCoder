package hello.crawler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import hello.domain.Main;
import hello.model.Problem;
import hello.model.ProblemId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sharath on 4/29/15.
 */
public class ProblemDetailsParser {
    private static final Logger log = LogManager.getLogger();
    public static void main(String[] args) throws IOException, InterruptedException {
        ProblemDetailsParser problemDetailsParser = new ProblemDetailsParser();
        try {
            problemDetailsParser.go();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    private void go() throws IOException, InterruptedException {
        //getAllProblemDetails();
        //fillProblemIdTable();
        saveToDatabase();

    }

    public void saveToDatabase() throws IOException {
        new Main.DbJob<Void>(session -> {
            try {
                Files.walkFileTree(Paths.get("/Users/sharath/projects/AllProblemDetailsHtml"), new Main.LambdaFileVisitor((path, blah) -> {
                    if(path.getFileName().toString().equals(".DS_Store")) return;
                    int id = Integer.parseInt(path.getFileName().toString());
                    Problem problem = (Problem)session.get(Problem.class, id);
                    problem.problemId = id;
                    try {
                        getProblemName(new String(Files.readAllBytes(path)), problem);
                    } catch (Exception e) {
                        throw new RuntimeException("error for "+id, e);
                    }
                    session.update(problem);
                }));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
           return null;
        }).doWork();

    }

    public void getAllProblemDetails() throws IOException, InterruptedException {
        //Files.createDirectory(Paths.get("AllProblemDetailsHtml"));
        TopcoderCrawler tcc = new TopcoderCrawler();

        tcc.authenticate();
        Session session = Main.in.getInstance(SessionFactory.class).openSession();

            while (true) {

                session.beginTransaction();
                String hql = "from ProblemId p where p.problemDetailHtml = false order by problemId desc";
                Query query = session.createQuery(hql);
                query.setMaxResults(1);
                if(query.list().size()==0) {
                    session.close();
                    break;
                }


                ProblemId problemId = (ProblemId) query.list().get(0);

                boolean error = false;
                try {
                    log.debug("trying to fetch {}, {}", problemId.className);
                    String detailsHtml = tcc.doGet(problemId.detailsHref);
                    log.debug("fetched problem details with count {}", detailsHtml.length());
                    Files.write(Paths.get("AllProblemDetailsHtml/" + problemId.problemId), detailsHtml.getBytes(), StandardOpenOption.CREATE);
                } catch (Exception e) {
                    log.error("error occured", e);
                    error = true;
                }
                if(!error) {
                    log.debug("trying to commit done status");
                    problemId.problemDetailHtml = true;
                    session.update(problemId);
                    session.getTransaction().commit();
                    log.debug("done with this problem");
                    Thread.sleep(4000);
                }


            }




    }

    public void fillProblemIdTable() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<ProblemId> list = mapper.readValue(Paths.get("hello.crawler.AllProblemsPageParser.problemIds").toFile(), new TypeReference<List<ProblemId>>() {});
        Session session = Main.in.getInstance(SessionFactory.class).openSession();
        session.beginTransaction();
        for (ProblemId problemId : list) {
            session.save(problemId);
        }
        session.getTransaction().commit();
        session.close();
        Main.in.getInstance(SessionFactory.class).close();

    }

    public Problem getProblemName(String html, Problem problem) {
        Document doc = Jsoup.parse(html);
        Element row = doc.select("td:containsOwn(Problem Name:)").get(0).parent();
        problem.className =  row.child(1).text();
        row = row.nextElementSibling();
        problem.matchName = row.child(1).text();
        row = row.nextElementSibling();
        String[] usedIn = Iterables.toArray(Splitter.on(",")
                .omitEmptyStrings()
                .trimResults()
                .split(row.child(1).text()), String.class);
        if(usedIn.length>2) throw new RuntimeException("Problem used in more than 2 divisions: "+ Arrays.toString(usedIn));
        Pattern p = Pattern.compile("Division ([I]+) Level (\\w+)");
        Function<String, Integer> getDivision = s -> ImmutableList.of("I", "II").indexOf(s)+1;
        Function<String, Integer> getLevel = s -> ImmutableList.of("One", "Two", "Three").indexOf(s)+1;
        Optional<Integer> d1Level = Optional.empty();
        Optional<Integer> d2Level = Optional.empty();
        for (int i = 0; i < usedIn.length; i++) {
            String s = usedIn[i];
            Matcher matcher = p.matcher(s);
            if(matcher.find()) {
                int div = getDivision.apply(matcher.group(1));
                int level = getLevel.apply(matcher.group(2));
                assert 1 <= div && div <= 2;
                assert 1 <= level && level <=3;
                if(div == 1) d1Level = Optional.of(level);
                if(div == 2) d2Level = Optional.of(level);
            }
        }
        d1Level.map(s -> problem.d1Level = s);
        d2Level.map(s -> problem.d2Level = s);

        row = row.nextElementSibling();
        String categories = row.child(1).text();
        problem.categories = categories;
        row = row.nextElementSibling();
        String writer = row.child(1).text();
        problem.writer = writer;

        row = row.nextElementSibling();
        String testers = row.child(1).text();
        problem.testers = testers;

        Function<String, String> stripLastChar = s -> s.substring(0, s.length()-1);

        Elements rows = row.parent().parent().nextElementSibling().child(0).children();
        String headerText = rows.get(0).child(1).text();

        String pointValue1 = rows.get(1).child(1).text();
        String competitors1 = rows.get(2).child(1).text();
        Double percentOpen1 =      Double.valueOf(stripLastChar.apply(rows.get(4).child(1).text()));
        Double percentSubmitted1 = Double.valueOf(stripLastChar.apply(rows.get(5).child(1).text()));
        Double percentCorrect1 =   Double.valueOf(stripLastChar.apply(rows.get(6).child(1).text()));

        setLevelDetails(problem, headerText, pointValue1, competitors1, percentOpen1, percentSubmitted1, percentCorrect1);

        Optional<String> headerText2 = Optional.empty();
        Optional<Integer> pointValue2 = Optional.empty();
        Optional<Integer> competitors2 = Optional.empty();
        Optional<Double> percentOpen2 =      Optional.empty();
        Optional<Double> percentSubmitted2 = Optional.empty();
        Optional<Double> percentCorrect2 =   Optional.empty();

        if(rows.get(1).children().size()==3) {
            headerText2 = Optional.of(rows.get(0).child(2).text());
            pointValue2 = Optional.of( Integer.parseInt(rows.get(1).child(2).text()));
            competitors2 = Optional.of(Integer.parseInt(rows.get(2).child(2).text()));
            percentOpen2 =      Optional.of(Double.valueOf(stripLastChar.apply(rows.get(4).child(2).text())));
            percentSubmitted2 = Optional.of(Double.valueOf(stripLastChar.apply(rows.get(5).child(2).text())));
            percentCorrect2 =   Optional.of(Double.valueOf(stripLastChar.apply(rows.get(6).child(2).text())));
        }

        if(headerText2.isPresent()){
            setLevelDetails(problem, headerText2.get(), pointValue2.get()+"", competitors2.get()+"", percentOpen2.get(), percentSubmitted2.get(), percentCorrect2.get());

        }
        rows = rows.get(0).parent().parent().nextElementSibling().child(0).children();
        int table3HeaderInd = 0;
        while(rows.get(table3HeaderInd).children().size()==0 || !rows.get(table3HeaderInd).child(0).text().startsWith("Division")) {
            table3HeaderInd++;
        }
        String table3Header = rows.get(table3HeaderInd).child(0).text();
        Function<String, Double> getseconds = s -> {
            if(Strings.isNullOrEmpty(s)) return 0.0;
            String[] ar = s.split(":");
            return Double.parseDouble(ar[0])*3600+Double.parseDouble(ar[1])*60+Double.parseDouble(ar[2]);
        };
        Elements averageCorrectTimeTds = doc.select("td:containsOwn(Average Correct Time)");
        Element averageCorrectTimeRow = averageCorrectTimeTds.get(0).parent();
        Double time1 = getseconds.apply(averageCorrectTimeRow.child(averageCorrectTimeRow.children().size()-1).text());
        if(table3Header.equals("Division I")) {
            problem.d1AverageCorrectSeconds = time1;
        } else if (table3Header.equals("Division II")){
            problem.d2AverageCorrectSeconds = time1;
        } else throw new RuntimeException("Unknown header value "+table3Header);

        Optional<String> table3Header2 = Optional.empty();
        Optional<Double> time2 = Optional.empty();
        int table3HeaderInd2 = table3HeaderInd+1;
        while(table3HeaderInd2<rows.size() && (rows.get(table3HeaderInd2).children().size()==0 || !rows.get(table3HeaderInd2).child(0).text().startsWith("Division"))) {
            table3HeaderInd2++;
        }

        if(table3HeaderInd2<rows.size()) {
            table3Header2 = Optional.of(rows.get(table3HeaderInd2).child(0).text());
            if(averageCorrectTimeTds.size()>1) {

                Element secondRow = averageCorrectTimeTds.get(1).parent();
                time2 = Optional.of(getseconds.apply(secondRow.child(secondRow.children().size() - 1).text()));
            }
        }
        if(table3Header2.isPresent()) {
            if(table3Header2.get().equals("Division I")) {
                problem.d2AverageCorrectSeconds = time2.get();
            } else if (table3Header2.get().equals("Division II")){
                problem.d2AverageCorrectSeconds = time2.get();
            } else throw new RuntimeException("Unknown header value "+table3Header2);
        }
        log.debug("{}, {}, {}, {}, {}", table3Header, table3Header2, time1, time2);
        return null;
    }


    private void setLevelDetails(Problem problem, String headerText, String pointValue1, String competitors1, Double percentOpen1, Double percentSubmitted1, Double percentCorrect1) {
        if(headerText.equals("Division I")) {
            problem.d1PointValue = Integer.parseInt(pointValue1);
            problem.d1Competitors = Integer.parseInt(competitors1);
            problem.d1PercentOpen = percentOpen1;
            problem.d1PercentSubmitted = percentSubmitted1;
            problem.d1PercentCorrect = percentCorrect1;
        } else if(headerText.equals("Division II")) {
            problem.d2PointValue = Integer.parseInt(pointValue1);
            problem.d2Competitors = Integer.parseInt(competitors1);
            problem.d2PercentOpen = percentOpen1;
            problem.d2PercentSubmitted = percentSubmitted1;
            problem.d2PercentCorrect = percentCorrect1;
        } else {
            throw new RuntimeException("Invalid value for division");
        }
    }
}
