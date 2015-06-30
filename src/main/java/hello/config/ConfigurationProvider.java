package hello.config;

import hello.model.Problem;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;

import javax.inject.Provider;
import javax.persistence.Entity;
import java.util.Set;

/**
 * Created by sharath on 4/27/15.
 */
public class ConfigurationProvider implements Provider<Configuration> {
    @Override
    public Configuration get() {
        Configuration cfg = new Configuration();
        final Reflections reflections = new Reflections("hello.model");
        final Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Entity.class);
        classes.forEach(cfg::addAnnotatedClass);
        return cfg
                .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect")
                .setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver")
                .setProperty("hibernate.connection.url", "jdbc:mysql://127.0.0.1/sharathdb")
                .setProperty("hibernate.connection.username", "sharath")
                //.configure()
                ;
    }
}
