package Graph;

public enum OperatorType {

    PROJECTION("π"),
    SELECTION("σ"),
    JOIN("⋈"),
    CROSS("×"),
    TABLE("");

    private final String symbol;

    OperatorType(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
