package hello.crawler;

import com.google.common.base.Strings;
import hello.domain.CaseTester;
import hello.domain.Main;
import hello.model.CoderSolutions;
import hello.model.FastestJavaSolutionLink;
import hello.model.Problem;
import hello.model.ProblemId;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by sharath on 5/7/15.
 */
public class DownloadFastestJava {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws IOException, InterruptedException {
        DownloadFastestJava dfj = new DownloadFastestJava();
        try {
            dfj.go();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    private void go() throws IOException, InterruptedException {
        //computeFastestHrefs();
        //downloadFastestJava();
        saveInDatabase();
        //saveSingleHtml(Paths.get("/Users/sharath/projects/TopCoder/FastestJavaSolutions/40"));
    }

    public void saveInDatabase() throws IOException {
        new Main.DbJob<Void>(session -> {
            try {
                int[] ind = new int[1];
                List<String> failed = new ArrayList<>();
                Files.walkFileTree(Paths.get("/Users/sharath/projects/TopCoder/FastestJavaSolutions"), new Main.LambdaFileVisitor((path, blah) -> {
                    try {
                        if(path.toString().contains("DS_Store")) return;
                        int id = Integer.parseInt(path.getFileName().toString());
                        Problem problem = (Problem) session.get(Problem.class, id);
                        String str = new String(Files.readAllBytes(path));
                        Document doc = Jsoup.parse(str);
                        if(doc.select("span:containsOwn(Sorry, there was an error in your request)").size()>0) {
                            Files.delete(path);
                            return;
                        }
                        Elements codeElements = doc.select("td:containsOwn(public)");
                        if(codeElements.size()==0) {
                            codeElements = doc.select("pre:containsOwn(public)");
                        }
                        //String code = new HtmlToPlainText().getPlainText(codeElements.get(0));
                        String code = codeElements.get(0).html();
                        code = StringEscapeUtils.unescapeHtml4(code);
                        code = code.replace('\u00a0', ' ')
                        .replace("<br>", "\n");



                        code = Main.formatJavaCode(code);
                        code = code.replace("// Powered by FileEdit", "");
                        code = code.replace("// Powered by CodeProcessor", "");
                        log.debug("processed {}, failed {}", ind[0]++, failed.size());
                        CaseTester caseTester = new CaseTester();
                        Optional<Boolean> oSkip = Optional.empty();
                        try {
                            caseTester.compile(code, problem.className);
                        } catch (Exception e) {
                            failed.add(problem.className);
                            log.debug(problem.className);
                            oSkip = Optional.of(false);
                        }


                        CoderSolutions.Pk pk = new CoderSolutions.Pk();
                        pk.problemId = id;
                        pk.coderHandle = FastestJavaSolutionLink.forProblemId(session, id).coder;
                        CoderSolutions cs = (CoderSolutions) session.get(CoderSolutions.class, pk);
                        cs.code = code.getBytes();
                        if(oSkip.isPresent()) {
                            problem.skip = oSkip.get();
                            session.update(problem);
                        }
                        session.update(cs);
                        if(ind[0]%20==0) {
                            session.flush();
                            session.clear();
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
                log.debug("{}", failed);
                log.debug("{}", failed.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;

        }).doWork();
    }


    public void downloadFastestJava() throws InterruptedException {
        TopcoderCrawler tcc = new TopcoderCrawler();

        tcc.authenticate();
        Session session = Main.in.getInstance(SessionFactory.class).openSession();

        while (true) {
            session.beginTransaction();
            String hql = "from FastestJavaSolutionLink f where f.done = false and f.link <> 'NOTHING' order by problemId desc";
            Query query = session.createQuery(hql);
            query.setMaxResults(1);
            if(query.list().size()==0) {
                session.close();
                break;
            }
            FastestJavaSolutionLink fastestJavaSolutionLink = (FastestJavaSolutionLink) query.list().get(0);
            boolean error = false;
            try {
                log.debug("trying to fetch {}, {}", fastestJavaSolutionLink.problemId, fastestJavaSolutionLink.link);
                String codePages = tcc.doGet(fastestJavaSolutionLink.link);
                log.debug("fetched code with count {}", codePages.length());
                Files.write(Paths.get("FastestJavaSolutions/" + fastestJavaSolutionLink.problemId), codePages.getBytes(), StandardOpenOption.CREATE);
            } catch (Exception e) {
                log.error("error occured", e);
                error = true;
            }
            if(!error) {
                log.debug("trying to commit done status");
                fastestJavaSolutionLink.done = true;
                session.update(fastestJavaSolutionLink);
                session.getTransaction().commit();
                log.debug("done with this problem");
                Thread.sleep(4000);
            }
        }
    }

    public void computeFastestHrefs() {
        new Main.DbJob<Void>(session -> {
            try {
                Files.walkFileTree(Paths.get("/Users/sharath/projects/AllProblemDetailsHtml"), new Main.LambdaFileVisitor((p, blah) -> {
                    try {
                        if(p.toString().endsWith(".DS_Store")) return;
                        processProblemDetail(p, session);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }).doWork();

    }

    /**
     * Parses a single problem details page into FastestJavaSolutionLink and updates in database
     * @param path
     * @param session
     * @throws IOException
     * @throws URISyntaxException
     */
    private void processProblemDetail(Path path, Session session) throws IOException, URISyntaxException {
        Document doc = Jsoup.parse(path.toFile(), Charset.defaultCharset().name());
        //log.debug("{}", path);
        Elements fastestTd = doc.select("td:containsOwn(Fastest)");
        if(fastestTd.size()==0) {
            fastestTd = doc.select("td:containsOwn(High Scorer)");
        }
        String coder = fastestTd.get(0).nextElementSibling().text();
        Element topJavaTd = doc.select("td:containsOwn(Top Submission)").get(0).nextElementSibling();
        String href = "NOTHING";
        if(topJavaTd.children().size()>0) {
            href =  "http://community.topcoder.com"+topJavaTd.child(0).attr("href");
        }
        // some solutions have an empty cr attribute (some invalid problems)
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(href), "UTF-8");
        for (NameValuePair param : params) {
            if(param.getName().equals("cr") && Strings.isNullOrEmpty(param.getValue())) {
                href = "NOTHING";
            }
        }
        int problemId = Integer.parseInt(path.getFileName().toString());
        log.debug("{}, {}, {}", problemId, coder, href);
        FastestJavaSolutionLink fs = FastestJavaSolutionLink.forProblemId(session, problemId);
        fs.coder = coder;
        session.update(fs);
    }

}
