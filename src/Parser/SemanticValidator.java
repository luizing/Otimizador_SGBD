
package Parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

            validateWhere(
                    join.condition(),
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

        // 5. Validar a ligação entre as tabelas, com os nomes já conferidos
        if (errors.isEmpty()) {
            validateJoinOrder(query, errors);
        }

        // Uma tabela inexistente não precisa ser informada duas vezes
        return new ArrayList<>(new LinkedHashSet<>(errors));
    }

    // Cada JOIN precisa ligar a tabela nova a uma tabela já disponível;
    // sem isso, a junção equivale a um produto cartesiano.
    private static void validateJoinOrder(
            QuerryObj query,
            List<String> errors
    ) {
        Set<String> available = new HashSet<>();
        available.add(normalize(query.getTables().get(0)));

        for (JoinTuple join : query.getJoins()) {

            String newTable = normalize(join.table());

            if (available.contains(newTable)) {
                errors.add(
                        "Tabela repetida no JOIN: " + join.table() +
                                ". Apelidos de tabela não fazem parte do trabalho."
                );
                continue;
            }

            Set<String> used = new HashSet<>();
            collectTables(join.condition(), query.getTables(), used);

            boolean usesNewTable = used.remove(newTable);

            Set<String> future = new HashSet<>(used);
            future.removeAll(available);

            if (!future.isEmpty()) {
                for (String table : future) {
                    errors.add(
                            "O ON do JOIN com " + join.table() +
                                    " usa a tabela " + table +
                                    ", que só é juntada depois deste JOIN."
                    );
                }
            } else if (!usesNewTable || used.isEmpty()) {
                errors.add(
                        "O ON do JOIN com " + join.table() +
                                " precisa relacionar essa tabela" +
                                " a uma tabela anterior da consulta."
                );
            }

            available.add(newTable);
        }
    }

    private static void collectTables(
            WhereClauses condition,
            List<String> tables,
            Set<String> found
    ) {
        if (condition.isComposite()) {
            collectTables(condition.getLeft(), tables, found);
            collectTables(condition.getRight(), tables, found);
            return;
        }

        collectTable(condition.getAtt1(), tables, found);
        collectTable(condition.getAtt2(), tables, found);
    }

    private static void collectTable(
            Operand operand,
            List<String> tables,
            Set<String> found
    ) {
        if (operand.type() != OperandType.IDENTIFIER) {
            return;
        }

        String[] parts = operand.value().split("\\.", 2);

        if (parts.length == 2) {
            found.add(normalize(parts[0]));
        } else {
            DataBase.findTablesWithColumn(parts[0], tables)
                    .forEach(table -> found.add(normalize(table)));
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
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
