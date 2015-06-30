package hello.config;

import com.google.inject.Inject;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.inject.Provider;

/**
 * Created by sharath on 4/28/15.
 */
public class SchemaExportProvider implements Provider<SchemaExport> {

    private Configuration cfg;

    @Inject
    public SchemaExportProvider(Configuration cfg) {
        this.cfg = cfg;
    }

    @Override
    public SchemaExport get() {
        SchemaExport export=new SchemaExport(cfg);
        return export;
    }
}
