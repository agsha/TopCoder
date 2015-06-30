package hello.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import javax.inject.Inject;
import javax.inject.Provider;


/**
 * Created by sharath on 5/6/15.
 */
public class SessionFactoryProvider implements Provider<SessionFactory> {
    Configuration configuration;
    private static final Logger log = LogManager.getLogger();


    @Inject
    public SessionFactoryProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SessionFactory get() {
        log.debug("yooooooo");
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().
                applySettings(configuration.getProperties());
        SessionFactory factory = configuration.buildSessionFactory(builder.build());
        return factory;
    }
}
