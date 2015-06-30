package hello.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by sharath on 5/10/15.
 */
@Entity
public class CoderSolutions {
    @EmbeddedId
    public Pk pk;
    public byte[] code;
    public String extra;

    @ManyToOne
    @JoinColumn(name="problemId")
    @MapsId("problemId")
    private Problem problem;

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
        problem.coderSolutions.add(this);
    }

    @Embeddable
    public static class Pk implements Serializable {
        public int problemId;
        public String coderHandle;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk)) return false;

            Pk that = (Pk) o;

            if (problemId != that.problemId) return false;
            return coderHandle.equals(that.coderHandle);

        }

        @Override
        public int hashCode() {
            int result = problemId;
            result = 31 * result + coderHandle.hashCode();
            return result;
        }
    }
}
