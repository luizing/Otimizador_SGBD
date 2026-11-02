package Parser;

public class WhereClauses {

    private Operand att1;
    private String relationship;
    private Operand att2;

    private WhereClauses left;
    private WhereClauses right;
    private String logicalOperator;

    // Condição simples
    public WhereClauses(Operand att1, String relationship, Operand att2) {
        this.att1 = att1;
        this.relationship = relationship;
        this.att2 = att2;
    }

    // Condição composta
    public WhereClauses(
            WhereClauses left,
            String logicalOperator,
            WhereClauses right
    ) {
        this.left = left;
        this.logicalOperator = logicalOperator;
        this.right = right;
    }

    public Operand getAtt1() {
        return att1;
    }

    public String getRelationship() {
        return relationship;
    }

    public Operand getAtt2() {
        return att2;
    }

    public WhereClauses getLeft() {
        return left;
    }

    public WhereClauses getRight() {
        return right;
    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public boolean isComposite() {
        return logicalOperator != null;
    }

    @Override
    public String toString() {
        if (isComposite()) {
            return "(" + left + " " + logicalOperator + " " + right + ")";
        }

        return att1 + " " + relationship + " " + att2;
    }
}