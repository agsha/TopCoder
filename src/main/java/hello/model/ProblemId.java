package hello.model;

import com.google.common.base.MoreObjects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Objects;

/**
 * Created by sharath on 5/6/15.
 */
@Entity
public class ProblemId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public int index;

    public String className;
    public String date;
    public int matchId;

    public int problemId;
    public boolean problemDetailHtml;
    public String detailsHref;

    public ProblemId(String className, String date, int problemId, String detailsHref, int matchId) {
        this.className = className;
        this.date = date;
        this.problemId = problemId;
        this.detailsHref = detailsHref;
        this.matchId = matchId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("index", index).add("classname", className).add("date", date)
                .add("problemid", problemId).add("matchId", matchId).toString();
    }

    public ProblemId() {
    }
}
