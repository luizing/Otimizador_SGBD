package Parser;

import java.util.*;

public class DataBase {

    private static final Map<String, Set<String>> schema = new HashMap<>();

    static {
        schema.put("Categoria", Set.of(
                "idCategoria", "Descricao"
        ));

        schema.put("Produto", Set.of(
                "idProduto", "Nome", "Descricao", "Preco",
                "QuantEstoque", "Categoria_idCategoria"
        ));

        schema.put("TipoCliente", Set.of(
                "idTipoCliente", "Descricao"
        ));

        schema.put("Cliente", Set.of(
                "idCliente", "Nome", "Email", "Nascimento",
                "Senha", "TipoCliente_idTipoCliente", "DataRegistro"
        ));

        schema.put("TipoEndereco", Set.of(
                "idTipoEndereco", "Descricao"
        ));

        schema.put("Endereco", Set.of(
                "idEndereco", "EnderecoPadrao", "Logradouro",
                "Numero", "Complemento", "Bairro", "Cidade",
                "UF", "CEP", "TipoEndereco_idTipoEndereco",
                "Cliente_idCliente"
        ));

        schema.put("Telefone", Set.of(
                "Numero", "Cliente_idCliente"
        ));

        schema.put("Status", Set.of(
                "idStatus", "Descricao"
        ));

        schema.put("Pedido", Set.of(
                "idPedido", "Status_idStatus", "DataPedido",
                "ValorTotalPedido", "Cliente_idCliente"
        ));

        schema.put("Pedido_has_Produto", Set.of(
                "idPedidoProduto", "Pedido_idPedido",
                "Produto_idProduto", "Quantidade", "PrecoUnitario"
        ));
    }

    // Normaliza nomes para ignorar diferenças entre maiúsculas e minúsculas
    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    // Retorna o nome da tabela exatamente como está no Modelo Relacional
    public static String canonicalTable(String table) {

        String normalizedTable = normalize(table);

        return schema.keySet().stream()
                .filter(t -> normalize(t).equals(normalizedTable))
                .findFirst()
                .orElse(table);
    }

    // Retorna o nome da coluna exatamente como está no Modelo Relacional
    public static String canonicalColumn(String table, String column) {

        String normalizedColumn = normalize(column);

        return schema.getOrDefault(canonicalTable(table), Set.of()).stream()
                .filter(c -> normalize(c).equals(normalizedColumn))
                .findFirst()
                .orElse(column);
    }

    // Verifica se uma tabela existe
    public static boolean existsTable(String table) {

        String normalizedTable = normalize(table);

        return schema.keySet().stream()
                .anyMatch(t -> normalize(t).equals(normalizedTable));
    }

    // Verifica se uma coluna existe em determinada tabela
    public static boolean existsColumn(String table, String column) {

        String normalizedTable = normalize(table);
        String normalizedColumn = normalize(column);

        Set<String> columns = schema.entrySet().stream()
                .filter(entry ->
                        normalize(entry.getKey()).equals(normalizedTable))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (columns == null) {
            return false;
        }

        return columns.stream()
                .anyMatch(c -> normalize(c).equals(normalizedColumn));
    }

    // Retorna as tabelas que possuem determinada coluna
    public static List<String> findTablesWithColumn(
            String column,
            List<String> tables
    ) {
        List<String> found = new ArrayList<>();

        for (String table : tables) {
            if (existsColumn(table, column)) {
                found.add(table);
            }
        }

        return found;
    }

    // Valida a estrutura da consulta
    public static List<String> validateQuery(QuerryObj query) {

        List<String> errors = new ArrayList<>();

        // Verifica as tabelas
        for (String table : query.getTables()) {

            if (!existsTable(table)) {
                errors.add("Tabela não encontrada: " + table);
            }
        }

        // Verifica as colunas
        for (ColumnObj column : query.getColumns()) {

            // SELECT *
            if (column.Column().equals("*")) {
                continue;
            }

            // Verifica se a tabela foi identificada
            if (column.table() == null) {
                errors.add("Coluna não encontrada: " + column.Column());
                continue;
            }

            // Verifica se a tabela existe
            if (!existsTable(column.table())) {
                errors.add("Tabela não encontrada: " + column.table());
                continue;
            }

            // Verifica se a tabela foi declarada no FROM
            boolean tableDeclared = query.getTables().stream()
                    .anyMatch(t ->
                            normalize(t).equals(normalize(column.table()))
                    );

            if (!tableDeclared) {
                errors.add(
                        "Tabela não declarada no FROM: " + column.table()
                );
                continue;
            }

            // Verifica se a coluna pertence à tabela
            if (!existsColumn(column.table(), column.Column())) {
                errors.add(
                        "Coluna " + column.Column() +
                                " não encontrada na tabela " + column.table()
                );
            }
        }

        return errors;
    }
}