package RelationalAlgebra;

import Parser.ColumnObj;
import Parser.JoinTuple;
import Parser.Operand;
import Parser.OperandType;
import Parser.QuerryObj;
import Parser.WhereClauses;

import java.util.List;
import java.util.stream.Collectors;

public class RelationalAlgebraConverter {

    public RelationalExpression convert(QuerryObj query) {

        if (query == null) {
            throw new IllegalArgumentException(
                    "A consulta não pode ser nula."
            );
        }

        if (query.getTables().isEmpty()) {
            throw new IllegalArgumentException(
                    "A consulta precisa possuir pelo menos uma tabela."
            );
        }

        // 1. Cria a primeira tabela da consulta
        RelationalExpression expression =
                RelationalExpression.table(
                        query.getTables().get(0)
                );

        // 2. Constrói as junções
        for (JoinTuple join : query.getJoins()) {

            RelationalExpression newTable =
                    RelationalExpression.table(join.table());

            String condition = formatCondition(join.condition());

            expression = RelationalExpression.join(
                    condition,
                    expression,
                    newTable
            );
        }

        // 3. Aplica as seleções (WHERE)
        for (WhereClauses where : query.getWheres()) {

            String condition = formatCondition(where);

            expression = RelationalExpression.selection(
                    condition,
                    expression
            );
        }

        // 4. Aplica a projeção (SELECT)
        String attributes = query.getColumns()
                .stream()
                .map(this::formatColumn)
                .collect(Collectors.joining(", "));

        expression = RelationalExpression.projection(
                attributes,
                expression
        );

        return expression;
    }

    // Formata uma coluna
    private String formatColumn(ColumnObj column) {

        if (column.table() == null) {
            return column.Column();
        }

        return column.table() + "." + column.Column();
    }

    // Formata as condições do WHERE
    private String formatCondition(WhereClauses condition) {

        if (condition.isComposite()) {

            return "(" +
                    formatCondition(condition.getLeft()) +
                    " " + condition.getLogicalOperator() + " " +
                    formatCondition(condition.getRight()) +
                    ")";
        }

        return formatOperand(condition.getAtt1()) +
                " " + condition.getRelationship() + " " +
                formatOperand(condition.getAtt2());
    }

    // Formata os operandos
    private String formatOperand(Operand operand) {

        if (operand.type() == OperandType.STRING) {

            return "'" +
                    operand.value().replace("'", "''") +
                    "'";
        }

        return operand.value();
    }
}