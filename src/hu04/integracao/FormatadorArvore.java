package hu04.integracao;

import hu04.modelo.NoOperador;

/**
 * Escreve a arvore otimizada na mesma notacao usada pela HU02.
 */
public class FormatadorArvore {

    public String formatar(NoOperador no) {
        if (no == null) {
            return "";
        }

        String esquerda = formatar(no.getEsquerdo());
        String direita = formatar(no.getDireito());

        return switch (no.getTipo()) {
            case TABELA -> no.getValor();

            case SELECAO ->
                "σ_{" + no.getValor() + "}(" + esquerda + ")";

            case PROJECAO ->
                "π_{" + no.getValor() + "}(" + esquerda + ")";

            case JUNCAO ->
                "(" + esquerda + " ⋈_{" + no.getValor() + "} "
                    + direita + ")";

            case PRODUTO_CARTESIANO ->
                "(" + esquerda + " × " + direita + ")";
        };
    }
}
