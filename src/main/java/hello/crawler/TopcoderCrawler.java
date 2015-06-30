package hello.crawler;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by sharath on 5/6/15.
 */
public class TopcoderCrawler {
    private static final Logger log = LogManager.getLogger();
    CloseableHttpClient httpclient;
    BasicCookieStore cookieStore;
    public static void main(String[] args) throws IOException, URISyntaxException {
        TopcoderCrawler tc = new TopcoderCrawler();
        tc.authenticate();
    }

    public TopcoderCrawler() {
        cookieStore = new BasicCookieStore();
        httpclient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    public void authenticate()  {
        try {
            HttpUriRequest login = RequestBuilder.post()
                    .setUri(new URI("http://community.topcoder.com/tc?module=Login"))
                    .addParameter("username", "vishnuosrn")
                    .addParameter("password", "Vishnuosrn1")
                    .build();
            CloseableHttpResponse response2 = httpclient.execute(login);
            EntityUtils.consume(response2.getEntity());
            response2.close();
            List<Cookie> cookies = cookieStore.getCookies();
            if (cookies.isEmpty()) {
                throw new RuntimeException("Empty cookies after logon attempt");
            } else {
                boolean ok = false;
                for (int i = 0; i < cookies.size(); i++) {
                    if(cookies.get(i).getName().equals("tcsso")) {
                        ok = true;
                    }
                }
                if(!ok) {
                    throw new RuntimeException("No cookie named tcsso after logon attempt");

                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String doGet(URL url, Map<String, String> params) throws URISyntaxException, IOException {
        URIBuilder builder = new URIBuilder(url.toURI());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.addParameter(entry.getKey(), entry.getValue());
        }
        CloseableHttpResponse response = httpclient.execute(new HttpGet(builder.build()));
        String res = EntityUtils.toString(response.getEntity());
        response.close();
        return res;
    }

    public String doGet(String url) {
        try {
            return doGet(new URL(url), ImmutableMap.of());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
