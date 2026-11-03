package RelationalAlgebra;

public class RelationalExpression {

    private final RelationalOperator operator;

    private final String value;

    private final RelationalExpression left;

    private final RelationalExpression right;

    public RelationalExpression(
            RelationalOperator operator,
            String value,
            RelationalExpression left,
            RelationalExpression right
    ) {
        this.operator = operator;
        this.value = value;
        this.left = left;
        this.right = right;
    }

    // Cria uma tabela
    public static RelationalExpression table(String name) {

        return new RelationalExpression(
                RelationalOperator.TABLE,
                name,
                null,
                null
        );
    }

    // Cria uma seleção
    public static RelationalExpression selection(
            String condition,
            RelationalExpression child
    ) {

        return new RelationalExpression(
                RelationalOperator.SELECTION,
                condition,
                child,
                null
        );
    }

    // Cria uma projeção
    public static RelationalExpression projection(
            String attributes,
            RelationalExpression child
    ) {

        return new RelationalExpression(
                RelationalOperator.PROJECTION,
                attributes,
                child,
                null
        );
    }

    // Cria uma junção
    public static RelationalExpression join(
            String condition,
            RelationalExpression left,
            RelationalExpression right
    ) {

        return new RelationalExpression(
                RelationalOperator.JOIN,
                condition,
                left,
                right
        );
    }

    public RelationalOperator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public RelationalExpression getLeft() {
        return left;
    }

    public RelationalExpression getRight() {
        return right;
    }
}