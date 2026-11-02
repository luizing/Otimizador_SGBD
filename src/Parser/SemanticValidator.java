
package Parser;

import java.util.ArrayList;
import java.util.List;

public class SemanticValidator {

    public static List<String> validate(QuerryObj query) {

        List<String> errors = new ArrayList<>();

        // 1. Validar tabelas
        for (String table : query.getTables()) {

            if (!DataBase.existsTable(table)) {
                errors.add("Tabela não encontrada: " + table);
            }
        }

        // 2. Validar colunas do SELECT
        for (ColumnObj column : query.getColumns()) {

            if (column.Column().equals("*")) {
                continue;
            }

            validateColumn(
                    column.table(),
                    column.Column(),
                    query.getTables(),
                    errors
            );
        }

        // 3. Validar JOINs
        for (JoinTuple join : query.getJoins()) {

            validateReference(
                    join.att1(),
                    query.getTables(),
                    errors
            );

            validateReference(
                    join.att2(),
                    query.getTables(),
                    errors
            );
        }

        // 4. Validar WHERE
        for (WhereClauses where : query.getWheres()) {
            validateWhere(
                    where,
                    query.getTables(),
                    errors
            );
        }

        return errors;
    }

    private static void validateWhere(
            WhereClauses where,
            List<String> tables,
            List<String> errors
    ) {

        // Condição composta: percorre os dois lados da árvore
        if (where.isComposite()) {

            validateWhere(where.getLeft(), tables, errors);
            validateWhere(where.getRight(), tables, errors);

            return;
        }

        // Primeiro operando
        validateOperand(
                where.getAtt1(),
                tables,
                errors
        );

        // Segundo operando
        validateOperand(
                where.getAtt2(),
                tables,
                errors
        );
    }

    private static void validateOperand(
            Operand operand,
            List<String> tables,
            List<String> errors
    ) {

        // Apenas identificadores representam referências a colunas.
        if (operand.type() == OperandType.IDENTIFIER) {

            validateReference(
                    operand.value(),
                    tables,
                    errors
            );
        }
    }

    private static void validateReference(
            String reference,
            List<String> tables,
            List<String> errors
    ) {

        String[] parts = reference.split("\\.", 2);

        if (parts.length == 2) {

            validateColumn(
                    parts[0],
                    parts[1],
                    tables,
                    errors
            );

        } else {

            validateColumn(
                    null,
                    reference,
                    tables,
                    errors
            );
        }
    }

    private static void validateColumn(
            String table,
            String column,
            List<String> tables,
            List<String> errors
    ) {

        if (table != null) {

            if (!DataBase.existsTable(table)) {
                errors.add("Tabela não encontrada: " + table);
                return;
            }

            boolean declared = tables.stream()
                    .anyMatch(t -> t.equalsIgnoreCase(table));

            if (!declared) {
                errors.add("Tabela não declarada no FROM: " + table);
                return;
            }

            if (!DataBase.existsColumn(table, column)) {
                errors.add(
                        "Coluna " + column +
                                " não encontrada na tabela " + table
                );
            }

            return;
        }

        // Coluna sem tabela explícita
        List<String> found = DataBase.findTablesWithColumn(
                column,
                tables
        );

        if (found.isEmpty()) {

            errors.add("Coluna não encontrada: " + column);

        } else if (found.size() > 1) {

            errors.add(
                    "Coluna ambígua: " + column +
                            ". Presente nas tabelas: " + found
            );
        }
    }
}
