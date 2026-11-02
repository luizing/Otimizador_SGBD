package Parser.lexer;

public record Token(
        TokenType type,
        String value,
        int position
) {}