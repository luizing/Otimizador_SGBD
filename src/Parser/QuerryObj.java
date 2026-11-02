package Parser;

import java.util.ArrayList;
import java.util.List;

public class QuerryObj {

    List<ColumnObj> columns = new ArrayList<>();
    List<String> tables = new ArrayList<>();
    List<JoinTuple> joins = new ArrayList<>();
    List<WhereClauses> wheres = new ArrayList<>();

    public void addColumns(List<ColumnObj> col) {
        this.columns.addAll(col);
    }

    public void addTables(List<String> tab) {
        this.tables.addAll(tab);
    }

    public void addWheres(List<WhereClauses> whe) {
        this.wheres.addAll(whe);
    }

    public List<ColumnObj> getColumns() {
        return columns;
    }

    public List<String> getTables() {
        return tables;
    }

    public List<WhereClauses> getWheres() {
        return wheres;
    }

    @Override
    public String toString() {
        return "QuerryObj{" +
                "columns=" + columns +
                ", tables=" + tables +
                ", joins=" + joins +
                ", wheres=" + wheres +
                '}';
    }
}