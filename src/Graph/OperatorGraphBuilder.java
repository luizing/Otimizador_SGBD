package Graph;

import Parser.QuerryObj;
import RelationalAlgebra.RelationalAlgebraConverter;
import RelationalAlgebra.RelationalExpression;

import java.util.List;

/**
 * Constrói o grafo NÃO otimizado a partir da álgebra relacional da HU2,
 * mantendo a ordem escrita na consulta:
 *
 *   π (raiz)
 *   └── σ (somente se houver WHERE)
 *       └── ⋈ (último JOIN)
 *           ├── ⋈ ... (JOINs anteriores)
 *           └── tabela do último JOIN
 */
public final class OperatorGraphBuilder {

    private OperatorGraphBuilder() {
    }

    public static OperatorNode build(QuerryObj query) {

        return build(new RelationalAlgebraConverter().convert(query));
    }

    public static OperatorNode build(RelationalExpression expression) {

        return switch (expression.getOperator()) {

            case TABLE -> new OperatorNode(
                    OperatorType.TABLE,
                    expression.getValue(),
                    List.of()
            );

            case SELECTION -> new OperatorNode(
                    OperatorType.SELECTION,
                    expression.getValue(),
                    List.of(build(expression.getLeft()))
            );

            case PROJECTION -> new OperatorNode(
                    OperatorType.PROJECTION,
                    expression.getValue(),
                    List.of(build(expression.getLeft()))
            );

            case JOIN -> new OperatorNode(
                    OperatorType.JOIN,
                    expression.getValue(),
                    List.of(
                            build(expression.getLeft()),
                            build(expression.getRight())
                    )
            );
        };
    }
}
