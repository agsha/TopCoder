package hello.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import hello.domain.Main;
import hello.model.Problem;
import hello.model.ProblemVO;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Slf4jRequestLog;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static spark.Spark.*;

/**
 * Created by sharath on 5/15/15.
 */
public class Server {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.go();
    }

    private void go() throws Exception {
        Controller controller = Main.in.getInstance(Controller.class);
        EmbeddedJetty.P p = Main.in.getInstance(EmbeddedJetty.P.class);
        List<EmbeddedJetty.LambdaServletInfo> list = new ArrayList<>();
        list.add(new EmbeddedJetty.LambdaServletInfo((req, resp) -> {
            try {
                resp.getWriter().write(controller.echo(req.getParameter("message")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "/hello"));
        list.add(new EmbeddedJetty.LambdaServletInfo((req, response) -> {
            try {
                Controller.GetStubFileForProblemRet ret = controller.getStubFileForProblem(req.getParameter("id"));

                response.setContentType("text/plain");
                response.setContentLength(ret.fileContent.getBytes().length);
                response.setHeader("Content-Disposition", "attachment; filename=\"" + ret.fileName + "\"");
                response.getWriter().write(ret.fileContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "/downloadProblem"));

//        list.add(new EmbeddedJetty.LambdaServletInfo((req, response) -> {
//            String javaFile = req.getParameter("javaFile");
//            log.debug("{}", javaFile);
//            try {
//                response.getWriter().write("done");
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }, "/upload"));

        list.add(new EmbeddedJetty.LambdaServletInfo((req, response) -> {
            try {
                List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                        String fieldName = item.getFieldName();
                        String fieldValue = item.getString();
                        // ... (do your job here)
                    } else {
                        // Process form file field (input type="file").
                        String fieldName = item.getFieldName();
                        String fileName = FilenameUtils.getName(item.getName());
                        InputStream fileContent = item.getInputStream();
                        String javaCode = IOUtils.toString(fileContent, "UTF-8");
                        log.debug("{}", javaCode);
                        response.getWriter().write("uploaded successfully");
                        Optional<String> result = controller.runSystemTest(javaCode, null);
                        if(result.isPresent()) {
                            response.getWriter().write(result.get());
                        } else {
                            response.getWriter().write("ALl tests passed");
                        }
                    }
                }
            } catch (FileUploadException e) {
                throw new RuntimeException("Cannot parse multipart request.", e);
            } catch (IOException e) {
                throw new RuntimeException("Cannot parse multipart request.", e);
            }
        }, "/upload"));


        list.add(new EmbeddedJetty.LambdaServletInfo((req, resp) -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                log.debug("query: {}", req.getParameter("query"));
                List<ProblemVO> results = controller.getResults(req.getParameter("query"));
                resp.getWriter().write(mapper.writeValueAsString(results));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "/problemsList"));
        EmbeddedJetty jetty = p.getFromLamdas(4567, list);
        Slf4jRequestLog requestLog = new Slf4jRequestLog();
        requestLog.setExtended(true);
        requestLog.setLogTimeZone("GMT");

        jetty.setRequestLog(requestLog);
        jetty.addStaticFolder("/Users/sharath/projects/Topcoder/src/main/resources/static");
        //log.debug(jetty.getHandler());
        jetty.start();
        jetty.join();
    }
}
