package hello.web;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by sgururaj on 2/8/15.
 */
public class EmbeddedJetty extends Server{
    private static final Logger log = LogManager.getLogger();

    private EmbeddedJetty(int port) {
        super(port);
    }

    public EmbeddedJetty addStaticFolder(String path) {
        HandlerCollection handlerCollection = (HandlerCollection) getHandler();
        if(getHandler()==null) {
            handlerCollection = new HandlerList();
            handlerCollection.setHandlers(new Handler[0]);
            setHandler(handlerCollection);
        }
        Handler[] handlers = handlerCollection.getHandlers();
        if(handlers == null) {
            handlers = new Handler[0];
            handlerCollection.setHandlers(handlers);
        }

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setResourceBase(path);
        Handler[] newHandlers = new Handler[handlers.length+1];
        System.arraycopy(handlers, 0, newHandlers, 1, handlers.length);
        newHandlers[0] = resource_handler;
        handlerCollection.setHandlers(newHandlers);
        return this;
    }


    @Singleton
    public static class P {
        private static final Logger log = LogManager.getLogger();

        public EmbeddedJetty get(int port, List<ServletInfo> servletInfo) {
            return get(port, "/", servletInfo);
        }

        public EmbeddedJetty getFromLamdas(int port, List<LambdaServletInfo> servletInfo) {
            List<ServletInfo> collect = servletInfo.stream().map(info -> new ServletInfo(new LambdaServlet(info.processor), info.urlPattern)).collect(Collectors.toList());
            return get(port, collect);
        }

        public EmbeddedJetty get(int port, String context, List<ServletInfo> servletInfo) {
            EmbeddedJetty server = new EmbeddedJetty(port);

            ServletContextHandler context0 = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context0.setContextPath(context);
            for (ServletInfo info : servletInfo) {
                context0.addServlet(new ServletHolder(info.servlet),info.urlPattern);
            }

            HandlerList handlers = new HandlerList();

            HandlerList handlerList = new HandlerList();
            handlerList.setHandlers(new Handler[] { context0 });

            server.setHandler(handlerList);
            return server;

        }

        public EmbeddedJetty get(int port, String urlPattern, HttpServlet servlet) {
            return get(port, ImmutableList.of(new ServletInfo(servlet, urlPattern)));
        }

        public EmbeddedJetty addStaticFolder(EmbeddedJetty server, String path) {
            Handler[] handlers = ((ContextHandlerCollection) server.getHandler()).getHandlers();
            ResourceHandler resource_handler = new ResourceHandler();
            resource_handler.setResourceBase(path);
            Handler[] newHandlers = new Handler[handlers.length+1];
            System.arraycopy(handlers, 0, newHandlers, 0, handlers.length);
            newHandlers[newHandlers.length-1] = resource_handler;
            ((ContextHandlerCollection) server.getHandler()).setHandlers(newHandlers);
            return server;
        }
    }

    public static class ServletInfo {
        public HttpServlet servlet;
        public String urlPattern;
        public ServletInfo(HttpServlet servlet, String urlPattern) {
            this.servlet = servlet;
            this.urlPattern = urlPattern;
        }
    }

    public static class LambdaServletInfo {
        public BiConsumer<HttpServletRequest, HttpServletResponse> processor;
        public String urlPattern;
        public LambdaServletInfo(BiConsumer<HttpServletRequest, HttpServletResponse> processor, String urlPattern) {
            this.processor = processor;
            this.urlPattern = urlPattern;
        }
    }

    public static class LambdaServlet extends HttpServlet {
        BiConsumer<HttpServletRequest, HttpServletResponse> processor;

        public LambdaServlet(BiConsumer<HttpServletRequest, HttpServletResponse> processor) {
            this.processor = processor;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            processor.accept(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            processor.accept(req, resp);
        }
    }
}