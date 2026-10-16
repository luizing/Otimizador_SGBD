package Parser;

import java.util.ArrayList;
import java.util.List;

public class QuerryObj {
    List<String> columns = new ArrayList<>();
    List<String> tables = new ArrayList<>();
    List<JoinTuple> joins = new ArrayList<>();
    List<WhereClauses> wheres = new ArrayList<>();

    public void addColumns(List<String> col){
        this.columns.addAll(col);
    }

    public void addTables(List<String> tab){
        this.tables.addAll(tab);
    }

    public void addWheres(List<WhereClauses> whe){
        this.wheres.addAll(whe);
    }

    @Override
    public String toString() {
        return "columns=" + columns +
                ", tables=" + tables +
                ", joins=" + joins +
                ", wheres=" + wheres +
                '}';
    }
}
