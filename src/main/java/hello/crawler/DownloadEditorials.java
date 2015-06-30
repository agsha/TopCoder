package hello.crawler;

import hello.domain.Main;
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

import static hello.domain.Main.l;

/**
 * Created by sharath on 5/14/15.
 */
public class DownloadEditorials {
    private static final Logger log = LogManager.getLogger();

    public TopcoderCrawler tcc;

    public static void main(String[] args) throws Exception {
        DownloadEditorials ed = new DownloadEditorials();
        ed.tcc = new TopcoderCrawler();
        ed.tcc.authenticate();
        try {
            ed.downloadAllEditorials();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }
    public void downloadEditorialPage() throws IOException {
        String html = tcc.doGet("http://apps.topcoder.com/wiki/display/tc/Algorithm+Problem+Set+Analysis");
        Files.write(Paths.get("/Users/sharath/projects/TopcoderEditorials/allEditorials.html"), html.getBytes());
    }
    private void downloadAllEditorials() throws IOException {
        Document doc = Jsoup.parse(new String(Files.readAllBytes(Paths.get("/Users/sharath/projects/TopcoderEditorials/allEditorials.html"))));
        Elements editorialDivs = doc.select("div.editorial-archive > div");
        log.debug(l(editorialDivs.size()));
    }
}
