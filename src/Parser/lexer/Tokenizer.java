package Parser.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Tokenizer {

    private final String input;
    private final List<Token> tokens = new ArrayList<>();

    private int position = 0;

    public Tokenizer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {

        while (position < input.length()) {

            char current = input.charAt(position);

            // Ignora espaços e quebras de linha
            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            // Identificadores e palavras-chave
            if (Character.isLetter(current) || current == '_') {
                readIdentifier();
                continue;
            }

            // Números
            if (Character.isDigit(current)) {
                readNumber();
                continue;
            }

            // Strings
            if (current == '\'') {
                readString();
                continue;
            }

            // Operadores e símbolos
            switch (current) {

                case '=' -> addToken(TokenType.EQUAL, "=");

                case '>' -> {
                    if (peekNext() == '=') {
                        addToken(TokenType.GREATER_EQUAL, ">=");
                        position++;
                    } else {
                        addToken(TokenType.GREATER, ">");
                    }
                }

                case '<' -> {
                    if (peekNext() == '=') {
                        addToken(TokenType.LESS_EQUAL, "<=");
                        position++;
                    } else if (peekNext() == '>') {
                        addToken(TokenType.NOT_EQUAL, "<>");
                        position++;
                    } else {
                        addToken(TokenType.LESS, "<");
                    }
                }

                case ',' -> addToken(TokenType.COMMA, ",");
                case '.' -> addToken(TokenType.DOT, ".");
                case '(' -> addToken(TokenType.OPEN_PAREN, "(");
                case ')' -> addToken(TokenType.CLOSE_PAREN, ")");
                case '*' -> addToken(TokenType.STAR, "*");
                case ';' -> addToken(TokenType.SEMICOLON, ";");

                default -> throw new IllegalArgumentException(
                        "Caractere inválido na posição " +
                                position + ": " + current
                );
            }

            position++;
        }

        tokens.add(new Token(TokenType.EOF, "", position));

        return tokens;
    }

    private void readIdentifier() {

        int start = position;

        while (position < input.length()) {

            char current = input.charAt(position);

            if (Character.isLetterOrDigit(current) || current == '_') {
                position++;
            } else {
                break;
            }
        }

        String value = input.substring(start, position);

        TokenType type = switch (value.toUpperCase(Locale.ROOT)) {
            case "SELECT" -> TokenType.SELECT;
            case "FROM" -> TokenType.FROM;
            case "WHERE" -> TokenType.WHERE;
            case "JOIN" -> TokenType.JOIN;
            case "ON" -> TokenType.ON;
            case "AND" -> TokenType.AND;
            default -> TokenType.IDENTIFIER;
        };

        tokens.add(new Token(type, value, start));
    }

    private void readNumber() {

        int start = position;

        while (position < input.length()
                && Character.isDigit(input.charAt(position))) {
            position++;
        }

        // Suporte a números decimais
        if (position < input.length()
                && input.charAt(position) == '.'
                && position + 1 < input.length()
                && Character.isDigit(input.charAt(position + 1))) {

            position++;

            while (position < input.length()
                    && Character.isDigit(input.charAt(position))) {
                position++;
            }
        }

        String value = input.substring(start, position);

        tokens.add(new Token(TokenType.NUMBER, value, start));
    }

    private void readString() {

        int start = position;
        position++;

        StringBuilder value = new StringBuilder();

        while (position < input.length()) {

            char current = input.charAt(position);

            if (current == '\'') {

                // Aspas duplicadas representam uma aspa dentro da string
                if (position + 1 < input.length()
                        && input.charAt(position + 1) == '\'') {

                    value.append('\'');
                    position += 2;
                    continue;
                }

                position++;

                tokens.add(new Token(
                        TokenType.STRING,
                        value.toString(),
                        start
                ));

                return;
            }

            value.append(current);
            position++;
        }

        throw new IllegalArgumentException(
                "String não finalizada na posição " + start
        );
    }

    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, position));
    }

    private char peekNext() {

        if (position + 1 >= input.length()) {
            return '\0';
        }

        return input.charAt(position + 1);
    }
}