package Parser;

import java.util.Arrays;

public class Parser {

    public static QuerryObj readRaw(String req){
        QuerryObj querry = new QuerryObj();

        // Select
        String[] partes = req.split("(?i)\\s+FROM\\s+");
        if (partes.length < 2) {
            throw new IllegalArgumentException("Consulta SQL inválida: FROM não encontrado.");
        }

        //adicionar checagem to select
        String colunasTexto = partes[0].replaceFirst("(?i)^\\s*SELECT\\s+", "");

        querry.addColumns(Arrays.stream(colunasTexto.split(","))
                .map(String::trim)
                .toList());

        // From
        String[] partesFrom = partes[1].split("(?i)\\s+WHERE\\s+", 2);

        //adicionar checagem to From
        String tablesTexto = partesFrom[0].trim();

        querry.addTables(Arrays.stream(tablesTexto.split("join"))
                .map(String::trim)
                .toList());

        // Where

        // multiplos ands
        if (partesFrom.length > 1) {
            String whereTexto = partesFrom[1].trim();
            querry.addTables(Arrays.stream(tablesTexto.split("and"))
                    .map(String::trim)
                    .toList());
        }

        return querry;
    }

}
