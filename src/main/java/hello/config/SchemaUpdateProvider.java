package hello.config;

import com.google.inject.Inject;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import javax.inject.Provider;

/**
 * Created by sharath on 4/28/15.
 */
public class SchemaUpdateProvider implements Provider<SchemaUpdate> {

    private Configuration cfg;

    @Inject
    public SchemaUpdateProvider(Configuration cfg) {
        this.cfg = cfg;
    }

    @Override
    public SchemaUpdate get() {
        SchemaUpdate updater=new SchemaUpdate(cfg);
        return updater;
    }
}
