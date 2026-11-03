package Graph;

import Parser.JoinTuple;
import Parser.QuerryObj;
import Parser.SemanticException;
import RelationalAlgebra.RelationalAlgebraConverter;
import RelationalAlgebra.RelationalExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

        validateJoinDependencies(query);

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

    // O ON só pode usar tabelas que já estejam disponíveis naquele ponto da
    // árvore; caso contrário, a junção dependeria de um resultado futuro.
    private static void validateJoinDependencies(QuerryObj query) {

        // A primeira tabela da lista é sempre a do FROM
        Set<String> available = new HashSet<>();
        available.add(normalize(query.getTables().get(0)));

        for (JoinTuple join : query.getJoins()) {

            available.add(normalize(join.table()));

            requireAvailable(join.att1(), join, available);
            requireAvailable(join.att2(), join, available);
        }
    }

    private static void requireAvailable(
            String reference,
            JoinTuple join,
            Set<String> available
    ) {
        String table = reference.split("\\.", 2)[0];

        if (!available.contains(normalize(table))) {
            throw new SemanticException(
                    "O ON do JOIN com " + join.table() +
                            " usa a tabela " + table +
                            ", que só é juntada depois deste JOIN."
            );
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
