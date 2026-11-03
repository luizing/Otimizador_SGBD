package RelationalAlgebra;

public class RelationalFormatter {

    public String format(RelationalExpression expression) {

        if (expression == null) {
            return "";
        }

        switch (expression.getOperator()) {

            case TABLE:

                return expression.getValue();

            case SELECTION:

                return "σ_{" +
                        expression.getValue() +
                        "}(" +
                        format(expression.getLeft()) +
                        ")";

            case PROJECTION:

                return "π_{" +
                        expression.getValue() +
                        "}(" +
                        format(expression.getLeft()) +
                        ")";

            case JOIN:

                return "(" +
                        format(expression.getLeft()) +
                        " ⋈_{" +
                        expression.getValue() +
                        "} " +
                        format(expression.getRight()) +
                        ")";

            default:

                throw new IllegalStateException(
                        "Operador desconhecido."
                );
        }
    }
}