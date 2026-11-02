package Parser;

import Parser.lexer.Token;
import Parser.lexer.TokenType;
import Parser.lexer.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private List<Token> tokens;
    private int position;

    public QuerryObj readRaw(String sql) {

        // 1. Tokenização
        Tokenizer tokenizer = new Tokenizer(sql);

        this.tokens = tokenizer.tokenize();
        this.position = 0;

        // 2. Criação da consulta
        QuerryObj query = new QuerryObj();

        // 3. Análise sintática
        parseSelect(query);
        parseFrom(query);
        parseJoins(query);
        parseWhere(query);

        match(TokenType.SEMICOLON);

        expect(
                TokenType.EOF,
                "Final inesperado da consulta."
        );

        // 4. Validação semântica
        List<String> errors = SemanticValidator.validate(query);

        if (!errors.isEmpty()) {
            throw new SemanticException(
                    String.join("; ", errors)
            );
        }

        return query;
    }

    // SELECT
    private void parseSelect(QuerryObj query) {

        expect(TokenType.SELECT, "Esperado SELECT.");

        List<ColumnObj> columns = new ArrayList<>();

        do {

            columns.add(parseColumn());

            if (!match(TokenType.COMMA)) {
                break;
            }

        } while (true);

        query.addColumns(columns);

        expect(TokenType.FROM, "Esperado FROM após as colunas.");
    }

    // Identifica uma coluna: Nome ou Cliente.Nome
    private ColumnObj parseColumn() {

        Token first = expect(
                TokenType.IDENTIFIER,
                "Esperado nome de coluna."
        );

        if (match(TokenType.DOT)) {

            Token second = expect(
                    TokenType.IDENTIFIER,
                    "Esperado nome da coluna após o ponto."
            );

            return new ColumnObj(first.value(), second.value());
        }

        return new ColumnObj(null, first.value());
    }

    // FROM
    private void parseFrom(QuerryObj query) {

        Token table = expect(
                TokenType.IDENTIFIER,
                "Esperado nome de tabela após FROM."
        );

        query.addTables(List.of(table.value()));
    }

    // JOIN
    private void parseJoins(QuerryObj query) {

        while (match(TokenType.JOIN)) {

            parseJoin(query);

        }
    }

    private void parseJoin(QuerryObj query) {

        Token table = expect(
                TokenType.IDENTIFIER,
                "Esperado nome da tabela após JOIN."
        );

        expect(
                TokenType.ON,
                "Esperado ON após o nome da tabela do JOIN."
        );

        ColumnObj att1 = parseColumn();

        Token operator = expectOperator();

        ColumnObj att2 = parseColumn();

        JoinTuple join = new JoinTuple(
                table.value(),
                att1.table() + "." + att1.Column(),
                operator.value(),
                att2.table() + "." + att2.Column()
        );

        query.addTables(List.of(table.value()));
        query.addJoin(join);
    }

    // WHERE
    private void parseWhere(QuerryObj query) {

        if (!match(TokenType.WHERE)) {
            return;
        }

        WhereClauses condition = parseLogicalCondition();

        query.addWheres(List.of(condition));
    }

    private WhereClauses parseLogicalCondition() {

        WhereClauses left = parsePrimaryCondition();

        while (match(TokenType.AND)) {

            WhereClauses right = parsePrimaryCondition();

            left = new WhereClauses(
                    left,
                    "AND",
                    right
            );
        }

        return left;
    }

    private WhereClauses parsePrimaryCondition() {

        if (match(TokenType.OPEN_PAREN)) {

            WhereClauses condition = parseLogicalCondition();

            expect(
                    TokenType.CLOSE_PAREN,
                    "Esperado ')' para fechar a condição."
            );

            return condition;
        }

        return parseCondition();
    }

    private WhereClauses parseCondition() {
        Operand att1 = parseOperand();

        Token operator = expectOperator();

        Operand att2 = parseOperand();

        return new WhereClauses(
                att1,
                operator.value(),
                att2
        );
    }

    private Operand parseOperand() {
        Token token = peek();

        if (match(TokenType.IDENTIFIER)) {
            String value = token.value();

            if (match(TokenType.DOT)) {
                Token column = expect(
                        TokenType.IDENTIFIER,
                        "Esperado nome da coluna após '.'."
                );

                value += "." + column.value();
            }

            return new Operand(value, OperandType.IDENTIFIER);
        }

        if (match(TokenType.NUMBER)) {
            return new Operand(token.value(), OperandType.NUMBER);
        }

        if (match(TokenType.STRING)) {
            return new Operand(token.value(), OperandType.STRING);
        }

        throw new SyntaxException(
                "Esperado identificador, número ou texto na posição " +
                        token.position()
        );
    }

    private Token expectOperator() {

        TokenType type = peek().type();

        switch (type) {

            case EQUAL:
            case GREATER:
            case LESS:
            case GREATER_EQUAL:
            case LESS_EQUAL:
            case NOT_EQUAL:
                position++;
                return tokens.get(position - 1);

            default:
                throw new SyntaxException(
                        "Erro sintático na posição " +
                                peek().position() +
                                ": operador inválido na condição ON."
                );
        }
    }

    // Verifica se o próximo token possui o tipo esperado
    private Token expect(TokenType type, String errorMessage) {

        Token current = peek();

        if (current.type() != type) {
            throw new SyntaxException(
                    "Erro sintático na posição " +
                            current.position() + ": " + errorMessage
            );
        }

        position++;

        return current;
    }

    // Verifica e consome um token caso corresponda
    private boolean match(TokenType type) {

        if (peek().type() == type) {
            position++;
            return true;
        }

        return false;
    }

    // Retorna o token atual
    private Token peek() {
        return tokens.get(position);
    }
}