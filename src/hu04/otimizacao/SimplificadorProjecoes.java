
package hu04.otimizacao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.LinkedHashSet;
import java.util.Set;

public class SimplificadorProjecoes {

    public static NoOperador simplificar(NoOperador raiz) {
        if (raiz == null) {
            return null;
        }

        return simplificarRecursivamente(raiz.copiar());
    }

    private static NoOperador simplificarRecursivamente(
            NoOperador no
    ) {
        if (no == null) {
            return null;
        }

        no.setEsquerdo(
                simplificarRecursivamente(no.getEsquerdo())
        );

        no.setDireito(
                simplificarRecursivamente(no.getDireito())
        );

        if (no.getTipo() != TipoOperador.PROJECAO
                || no.getDireito() != null
                || no.getAtributos().isEmpty()) {
            return no;
        }

        NoOperador filho = no.getEsquerdo();

        // Caso 1: duas projecoes consecutivas e equivalentes.
        if (filho != null
                && filho.getTipo() == TipoOperador.PROJECAO
                && filho.getDireito() == null
                && mesmosAtributos(no, filho)) {
            return filho;
        }

        // Caso 2: projecoes iguais separadas por uma selecao.
        if (filho != null
                && filho.getTipo() == TipoOperador.SELECAO
                && filho.getDireito() == null) {

            NoOperador projecaoInterna = filho.getEsquerdo();

            if (projecaoInterna != null
                    && projecaoInterna.getTipo()
                            == TipoOperador.PROJECAO
                    && projecaoInterna.getDireito() == null
                    && mesmosAtributos(no, projecaoInterna)) {
                return filho;
            }
        }

        return no;
    }

    private static boolean mesmosAtributos(
            NoOperador primeiro,
            NoOperador segundo
    ) {
        Set<String> atributosPrimeiro =
                new LinkedHashSet<>(primeiro.getAtributos());

        Set<String> atributosSegundo =
                new LinkedHashSet<>(segundo.getAtributos());

        return atributosPrimeiro.equals(atributosSegundo);
    }
}
