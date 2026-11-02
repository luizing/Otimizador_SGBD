package Parser;

import java.util.Arrays;
import java.util.List;

public class Parser {

    public static QuerryObj readRaw(String req) {

        QuerryObj querry = new QuerryObj();

        // SELECT
        String[] partes = req.split("(?i)\\s+FROM\\s+", 2);

        if (partes.length < 2) {
            throw new IllegalArgumentException(
                    "Consulta SQL inválida: FROM não encontrado."
            );
        }

        if (!partes[0].trim().matches("(?is)^SELECT\\s+.+")) {
            throw new IllegalArgumentException(
                    "Consulta SQL inválida: SELECT não encontrado."
            );
        }

        String colunasTexto = partes[0].replaceFirst(
                "(?i)^\\s*SELECT\\s+", ""
        );

        querry.addColumns(
                Arrays.stream(colunasTexto.split(","))
                        .map(String::trim)
                        .map(coluna -> {

                            String[] partesColuna = coluna.split("\\.", 2);

                            if (partesColuna.length == 2) {
                                return new ColumnObj(
                                        partesColuna[0].trim(),
                                        partesColuna[1].trim()
                                );
                            }

                            return new ColumnObj(null, coluna.trim());

                        })
                        .toList()
        );

        // FROM
        String[] partesFrom = partes[1].split(
                "(?i)\\s+WHERE\\s+", 2
        );

        String tablesTexto = partesFrom[0].trim();

        querry.addTables(
                Arrays.stream(tablesTexto.split("(?i)\\s+JOIN\\s+"))
                        .map(String::trim)
                        .toList()
        );

        // WHERE
        if (partesFrom.length > 1) {

            String whereTexto = partesFrom[1].trim();

            String[] condicoes = whereTexto.split(
                    "(?i)\\s+AND\\s+"
            );

            for (String condicao : condicoes) {

                String[] elementos = condicao.split(
                        "\\s*(>=|<=|!=|<>|=|>|<)\\s*", 2
                );

                if (elementos.length < 2) {
                    throw new IllegalArgumentException(
                            "Condição WHERE inválida: " + condicao
                    );
                }

                String att1 = elementos[0].trim();
                String att2 = elementos[1].trim();

                String relacionamento = condicao.substring(
                        att1.length(),
                        condicao.length() - att2.length()
                ).trim();

                querry.addWheres(
                        Arrays.asList(
                                new WhereClauses(
                                        att1,
                                        relacionamento,
                                        att2
                                )
                        )
                );
            }
        }

        // Validação da consulta
        List<String> erros = DataBase.validateQuery(querry);

        if (!erros.isEmpty()) {
            throw new IllegalArgumentException(
                    "Erro na consulta SQL: " + String.join("; ", erros)
            );
        }

        return querry;
    }
}