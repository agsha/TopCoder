package hello.crawler;

import com.google.inject.Injector;
import hello.domain.Main;
import hello.model.MyTable;
import hello.model.MyTableNicknames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

/**
 * Created by sharath on 5/9/15.
 */
public class HibernateTester {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) {
        HibernateTester ht = new HibernateTester();
        try {
            ht.go();
        } finally {
            Main.in.getInstance(SessionFactory.class).close();
        }
    }

    private void go() {
        Injector in = Main.in;
        SessionFactory sf = in.getInstance(SessionFactory.class);
        Session session = sf.openSession();
        session.beginTransaction();
        MyTable t = new MyTable(6, "blah");
        MyTableNicknames mtn = new MyTableNicknames();
        mtn.myTable = t;
        t.myTableNicknames.add(mtn);
        mtn.pk = new MyTableNicknames.Pk();
        mtn.pk.nickName = "nickname3";
        session.saveOrUpdate(t);
        //session.saveOrUpdate(mtn);
        session.getTransaction().commit();
        printall();
    }

    public void printall() {
        Injector in = Main.in;
        SessionFactory sf = in.getInstance(SessionFactory.class);
        Session session = sf.openSession();
        Query query = session.createQuery("from MyTable");
        List<MyTable> list = query.list();
        for (MyTable myTable : list) {
            log.debug("{}", myTable);
        }
        session.close();
    }

}
