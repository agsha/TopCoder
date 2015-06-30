package hello.domain;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import hello.config.ConfigurationProvider;
import hello.config.SchemaExportProvider;
import hello.config.SchemaUpdateProvider;
import hello.config.SessionFactoryProvider;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

/**
 * Created by sharath on 4/28/15.
 */
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Configuration.class).toProvider(ConfigurationProvider.class);
        bind(SchemaExport.class).toProvider(SchemaExportProvider.class);
        bind(SchemaUpdate.class).toProvider(SchemaUpdateProvider.class);
        bind(SessionFactory.class).toProvider(SessionFactoryProvider.class).in(Singleton.class);

    }
}
