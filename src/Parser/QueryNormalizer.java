package Parser;

import java.util.List;

/**
 * Reescreve uma consulta ja validada com os nomes do Modelo Relacional e com
 * todas as colunas qualificadas. A partir daqui, "email", "CLIENTE.EMAIL" e
 * "cliente.email" sao a mesma referencia: Cliente.Email.
 */
final class QueryNormalizer {

    private QueryNormalizer() {
    }

    static QuerryObj normalize(QuerryObj query) {

        List<String> tables = query.getTables();
        QuerryObj normalized = new QuerryObj();

        normalized.addTables(tables.stream()
                .map(DataBase::canonicalTable)
                .toList());

        normalized.addColumns(query.getColumns().stream()
                .map(column -> toColumn(column, tables))
                .toList());

        for (JoinTuple join : query.getJoins()) {
            normalized.addJoin(new JoinTuple(
                    DataBase.canonicalTable(join.table()),
                    normalizeCondition(join.condition(), tables)
            ));
        }

        normalized.addWheres(query.getWheres().stream()
                .map(where -> normalizeCondition(where, tables))
                .toList());

        return normalized;
    }

    private static ColumnObj toColumn(ColumnObj column, List<String> tables) {

        String table = resolveTable(column.table(), column.Column(), tables);

        return new ColumnObj(
                table,
                DataBase.canonicalColumn(table, column.Column())
        );
    }

    private static WhereClauses normalizeCondition(
            WhereClauses condition,
            List<String> tables
    ) {
        if (condition.isComposite()) {
            return new WhereClauses(
                    normalizeCondition(condition.getLeft(), tables),
                    condition.getLogicalOperator(),
                    normalizeCondition(condition.getRight(), tables)
            );
        }

        return new WhereClauses(
                normalizeOperand(condition.getAtt1(), tables),
                condition.getRelationship(),
                normalizeOperand(condition.getAtt2(), tables)
        );
    }

    private static Operand normalizeOperand(
            Operand operand,
            List<String> tables
    ) {
        if (operand.type() != OperandType.IDENTIFIER) {
            return operand;
        }

        String[] parts = operand.value().split("\\.", 2);

        String tableName = parts.length == 2 ? parts[0] : null;
        String column = parts[parts.length - 1];

        String table = resolveTable(tableName, column, tables);

        return new Operand(
                table + "." + DataBase.canonicalColumn(table, column),
                OperandType.IDENTIFIER
        );
    }

    // A validacao semantica ja garantiu que a coluna existe e nao e ambigua.
    private static String resolveTable(
            String table,
            String column,
            List<String> tables
    ) {
        if (table != null) {
            return DataBase.canonicalTable(table);
        }

        return DataBase.canonicalTable(
                DataBase.findTablesWithColumn(column, tables).get(0)
        );
    }
}
