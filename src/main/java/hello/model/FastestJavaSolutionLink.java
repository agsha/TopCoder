package hello.model;

import org.hibernate.Session;

import javax.persistence.*;

/**
 * Created by sharath on 5/8/15.
 */
@NamedQueries(
        {
                @NamedQuery(
                        name = "byProblemId",
                        query = "from FastestJavaSolutionLink f where f.problemId = :pid"
                )
        }
)
@Entity
public class FastestJavaSolutionLink {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long id;
    public int problemId;
    public String link;
    public boolean done;
    public String coder;

    public FastestJavaSolutionLink() {
    }

    public FastestJavaSolutionLink(int problemId, String link, boolean done) {
        this.problemId = problemId;
        this.link = link;
        this.done = done;

    }

    public static FastestJavaSolutionLink forProblemId(Session session, int problemId) {
        return (FastestJavaSolutionLink) session.getNamedQuery("byProblemId").setParameter("pid", problemId).uniqueResult();
    }
}
