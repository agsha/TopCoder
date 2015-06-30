package hello.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sharath on 5/9/15.
 */
@Entity
public class MyTable {
    @Id
    public int id;
    public String name;

    @OneToMany(mappedBy="myTable")
    public Set<MyTableNicknames> myTableNicknames = new HashSet<>();

    public MyTable() {
    }

    public MyTable(int id, String name) {
        this.id = id;
        this.name = name;
    }



    @Override
    public String toString() {
        return "MyTable{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
