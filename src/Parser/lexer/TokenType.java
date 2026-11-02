package Parser.lexer;

public enum TokenType {

    // Palavras-chave
    SELECT,
    FROM,
    WHERE,
    JOIN,
    ON,
    AND,

    // Identificadores e valores
    IDENTIFIER,
    NUMBER,
    STRING,

    // Operadores
    EQUAL,
    GREATER,
    LESS,
    GREATER_EQUAL,
    LESS_EQUAL,
    NOT_EQUAL,

    // Símbolos
    COMMA,
    DOT,
    OPEN_PAREN,
    CLOSE_PAREN,
    STAR,
    SEMICOLON,

    EOF
}