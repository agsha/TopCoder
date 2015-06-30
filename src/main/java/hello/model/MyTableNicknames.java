package hello.model;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by sharath on 5/11/15.
 */
@Entity
public class MyTableNicknames {
    @EmbeddedId
    public Pk pk;

    @ManyToOne
    @JoinColumn(name = "myTableId")
    @MapsId("myTableId")
    public MyTable myTable;

    public String address;

    @Override
    public String toString() {
        return "MyTableNicknames{" +
                "pk=" + pk +
                ", myTable=" + myTable.id +
                ", address='" + address + '\'' +
                '}';
    }

    @Embeddable
    public static class Pk implements Serializable {
        public int myTableId;
        public String nickName;

        @Override
        public String toString() {
            return "Pk{" +
                    "myTableId=" + myTableId +
                    ", nickName='" + nickName + '\'' +
                    '}';
        }
    }

}
