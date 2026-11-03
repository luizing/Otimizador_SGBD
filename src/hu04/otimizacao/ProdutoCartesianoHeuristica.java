
package hu04.otimizacao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProdutoCartesianoHeuristica {

    private static final String IDENT = "[a-zA-Z_]\\w*";
    private static final String ATRIBUTO = IDENT + "\\." + IDENT;

    private static final Pattern CONDICAO_JUNCAO = Pattern.compile(
        "^\\s*(" + ATRIBUTO + ")\\s*=\\s*(" +
        ATRIBUTO + ")\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    public static NoOperador evitarProdutoCartesiano(
        NoOperador raiz
    ) {
        if (raiz == null) {
            return null;
        }

        return otimizar(raiz.copiar());
    }

    private static NoOperador otimizar(NoOperador no) {
        if (no == null) {
            return null;
        }

        no.setEsquerdo(otimizar(no.getEsquerdo()));
        no.setDireito(otimizar(no.getDireito()));

        if (no.getTipo() != TipoOperador.SELECAO) {
            return no;
        }

        if (no.getDireito() != null) {
            return no;
        }

        NoOperador filho = no.getEsquerdo();

        if (filho == null ||
                filho.getTipo() != TipoOperador.PRODUTO_CARTESIANO) {
            return no;
        }

        if (filho.getEsquerdo() == null ||
                filho.getDireito() == null) {
            return no;
        }

        Matcher matcher = CONDICAO_JUNCAO.matcher(
            no.getValor() == null ? "" : no.getValor()
        );

        if (!matcher.matches()) {
            return no;
        }

        String atributo1 = matcher.group(1);
        String atributo2 = matcher.group(2);

        String tabela1 = atributo1.split("\\.")[0];
        String tabela2 = atributo2.split("\\.")[0];

        if (tabela1.equalsIgnoreCase(tabela2)) {
            return no;
        }

        boolean tabela1Esquerda = contemTabela(
            filho.getEsquerdo(), tabela1
        );

        boolean tabela1Direita = contemTabela(
            filho.getDireito(), tabela1
        );

        boolean tabela2Esquerda = contemTabela(
            filho.getEsquerdo(), tabela2
        );

        boolean tabela2Direita = contemTabela(
            filho.getDireito(), tabela2
        );

        boolean normal = tabela1Esquerda && !tabela1Direita
            && tabela2Direita && !tabela2Esquerda;

        boolean inversa = tabela1Direita && !tabela1Esquerda
            && tabela2Esquerda && !tabela2Direita;

        if (!normal && !inversa) {
            return no;
        }

        NoOperador juncao = new NoOperador(
            TipoOperador.JUNCAO, no.getValor()
        );

        juncao.setEsquerdo(filho.getEsquerdo());
        juncao.setDireito(filho.getDireito());

        return juncao;
    }

    private static boolean contemTabela(
        NoOperador no, String tabela
    ) {
        if (no == null) {
            return false;
        }

        if (no.getTipo() == TipoOperador.TABELA) {
            return tabela.equalsIgnoreCase(no.getValor());
        }

        return contemTabela(no.getEsquerdo(), tabela)
            || contemTabela(no.getDireito(), tabela);
    }
}
