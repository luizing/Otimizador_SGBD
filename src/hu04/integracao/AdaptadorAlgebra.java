
package hu04.integracao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import RelationalAlgebra.RelationalExpression;
import RelationalAlgebra.RelationalOperator;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AdaptadorAlgebra {

    public NoOperador converter(RelationalExpression expressao) {
        Objects.requireNonNull(
                expressao,
                "A expressao relacional nao pode ser nula."
        );

        RelationalOperator operador = expressao.getOperator();

        if (operador == null) {
            throw new IllegalArgumentException(
                    "Operador relacional nao pode ser nulo."
            );
        }

        TipoOperador tipo = converterTipo(operador);

        NoOperador no = new NoOperador(
                tipo,
                expressao.getValue()
        );

        if (operador == RelationalOperator.PROJECTION) {
            no.setAtributos(
                    separarAtributos(expressao.getValue())
            );
        }

        if (expressao.getLeft() != null) {
            no.setEsquerdo(
                    converter(expressao.getLeft())
            );
        }

        if (expressao.getRight() != null) {
            no.setDireito(
                    converter(expressao.getRight())
            );
        }

        return no;
    }

    private TipoOperador converterTipo(
            RelationalOperator operador
    ) {
        return switch (operador) {
            case TABLE -> TipoOperador.TABELA;
            case SELECTION -> TipoOperador.SELECAO;
            case PROJECTION -> TipoOperador.PROJECAO;
            case JOIN -> TipoOperador.JUNCAO;
        };
    }

    private List<String> separarAtributos(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }

        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(atributo -> !atributo.isEmpty())
                .toList();
    }
}
