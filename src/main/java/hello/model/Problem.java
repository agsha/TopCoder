package hello.model;

import hello.domain.Main;
import org.hibernate.Session;

import javax.annotation.Generated;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sharath on 4/27/15.
 */
@Entity
public class Problem implements Serializable{
    @Id
    public  int problemId;
    public  int matchId;
    public Date date;
    public  String matchName;
    public  String categories;
    public  String writer;
    public  String testers;
    public  int d1Level;
    public  int d1PointValue;
    public  int d1Competitors;
    public  double d1PercentOpen;
    public  double d1PercentSubmitted;
    public  double d1PercentCorrect;
    public  double d1AverageCorrectSeconds;

    public  int d2Level;
    public  int d2PointValue;
    public  int d2Competitors;
    public  double d2PercentOpen;
    public  double d2PercentSubmitted;
    public  double d2PercentCorrect;
    public  double d2AverageCorrectSeconds;

    public  byte[] statementHtmlZip;
    public  byte[] editorialHtmlZip;
    public  String statementText;
    public  String className;
    public String signature;
    public  String methodName;
    public  String inputParamsCommaSeparated;
    public  String returnType;
    public  byte[] sysTestsJson;
    public  byte[] demoTestJson;
    public boolean skip;
    public String comments;
    @OneToMany(mappedBy = "problem")
    public Set<CoderSolutions> coderSolutions = new HashSet<>();

    public static Problem fromId(int problemId) {
        return new Main.DbJob<>(session -> fromId(session, problemId)).doWork();
    }

    public static Problem fromId(Session session, int problemId) {
        return (Problem) session.get(Problem.class, problemId);
    }

    public static Problem fromName(String className) {
        return new Main.DbJob<>(session -> fromName(session, className)).doWork();

    }

    public static Problem fromName(Session session, String className) {
        return (Problem) session.createQuery("from Problem p where p.className = :c").setParameter("c", className).uniqueResult();
    }
}
