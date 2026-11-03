
package hu04.integracao;

import Graph.OperatorNode;
import Graph.OperatorType;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdaptadorGrafico {

    public OperatorNode converter(NoOperador raiz) {
        Objects.requireNonNull(
                raiz,
                "A arvore otimizada nao pode ser nula."
        );

        OperatorType tipo = converterTipo(raiz.getTipo());

        List<OperatorNode> filhos = new ArrayList<>();

        if (raiz.getEsquerdo() != null) {
            filhos.add(converter(raiz.getEsquerdo()));
        }

        if (raiz.getDireito() != null) {
            filhos.add(converter(raiz.getDireito()));
        }

        return new OperatorNode(
                tipo,
                raiz.getValor(),
                filhos
        );
    }

    private OperatorType converterTipo(TipoOperador tipo) {
        return switch (tipo) {
            case TABELA -> OperatorType.TABLE;
            case SELECAO -> OperatorType.SELECTION;
            case PROJECAO -> OperatorType.PROJECTION;
            case JUNCAO -> OperatorType.JOIN;

            case PRODUTO_CARTESIANO ->
                    throw new IllegalArgumentException(
                            "A HU03 nao possui representacao para "
                            + "produto cartesiano."
                    );
        };
    }
}
