
package hu04.otimizacao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Heuristica {

    private static final String IDENT = "[a-zA-Z_]\\w*";
    private static final String ATRIBUTO = IDENT + "\\." + IDENT;

    private static final Pattern REFERENCIA = Pattern.compile(
        "\\b(" + IDENT + ")\\.(" + IDENT + ")\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONDICAO_SIMPLES = Pattern.compile(
        "^\\s*(" + ATRIBUTO + ")\\s*(<=|>=|<>|=|<|>)\\s*" +
        "(?:-?\\d+(?:\\.\\d+)?|'(?:''|[^'])*')\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CONDICAO_JUNCAO = Pattern.compile(
        "^\\s*(" + ATRIBUTO + ")\\s*=\\s*(" +
        ATRIBUTO + ")\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private record CondicaoJuncao(
        String texto, String tabela1, String tabela2
    ) {}

    public static NoOperador reduzirTuplas(NoOperador raiz) {
        if (raiz == null) {
            return null;
        }

        return otimizarSelecoes(raiz.copiar());
    }

    private static NoOperador otimizarSelecoes(NoOperador no) {
        if (no == null) {
            return null;
        }

        no.setEsquerdo(otimizarSelecoes(no.getEsquerdo()));
        no.setDireito(otimizarSelecoes(no.getDireito()));

        if (no.getTipo() != TipoOperador.SELECAO) {
            return no;
        }

        NoOperador filho = no.getEsquerdo();

        if (filho == null || no.getDireito() != null
                || filho.getTipo() != TipoOperador.JUNCAO) {
            return no;
        }

        String tabela = identificarTabela(no.getValor());

        if (tabela == null) {
            return no;
        }

        boolean esquerda = contemTabela(filho.getEsquerdo(), tabela);
        boolean direita = contemTabela(filho.getDireito(), tabela);

        if (esquerda == direita) {
            return no;
        }

        if (esquerda) {
            no.setEsquerdo(filho.getEsquerdo());
            filho.setEsquerdo(otimizarSelecoes(no));
        } else {
            no.setEsquerdo(filho.getDireito());
            filho.setDireito(otimizarSelecoes(no));
        }

        return filho;
    }

    public static NoOperador reduzirAtributos(NoOperador raiz) {
        if (raiz == null) {
            return null;
        }

        NoOperador copia = raiz.copiar();

        if (copia.getTipo() != TipoOperador.PROJECAO
                || copia.getEsquerdo() == null
                || copia.getDireito() != null
                || copia.getAtributos().isEmpty()) {
            return copia;
        }

        Set<String> necessarios = new LinkedHashSet<>();

        for (String atributo : copia.getAtributos()) {
            Matcher matcher = REFERENCIA.matcher(atributo);

            if (!matcher.matches()) {
                return copia;
            }

            necessarios.add(matcher.group());
        }

        if (!estruturaSuportada(copia.getEsquerdo())) {
            return copia;
        }

        copia.setEsquerdo(projetar(copia.getEsquerdo(), necessarios));
        return copia;
    }

    private static NoOperador projetar(
        NoOperador no, Set<String> necessarios
    ) {
        if (no.getTipo() == TipoOperador.JUNCAO) {
            Set<String> atributos = new LinkedHashSet<>(necessarios);
            atributos.addAll(extrairReferencias(no.getValor()));

            Set<String> esquerda = filtrarPorRamo(
                atributos, no.getEsquerdo()
            );

            Set<String> direita = filtrarPorRamo(
                atributos, no.getDireito()
            );

            no.setEsquerdo(projetar(no.getEsquerdo(), esquerda));
            no.setDireito(projetar(no.getDireito(), direita));

            return no;
        }

        if (no.getTipo() == TipoOperador.SELECAO) {
            Set<String> atributos = new LinkedHashSet<>(necessarios);
            atributos.addAll(extrairReferencias(no.getValor()));

            no.setEsquerdo(projetar(no.getEsquerdo(), atributos));
            return criarProjecao(no, necessarios);
        }

        if (no.getTipo() == TipoOperador.TABELA) {
            return criarProjecao(no, necessarios);
        }

        return no;
    }

    private static NoOperador criarProjecao(
        NoOperador filho, Set<String> atributos
    ) {
        if (atributos.isEmpty()) {
            return filho;
        }

        List<String> lista = new ArrayList<>(atributos);

        NoOperador projecao = new NoOperador(
            TipoOperador.PROJECAO, String.join(", ", lista)
        );

        projecao.setAtributos(lista);
        projecao.setEsquerdo(filho);

        return projecao;
    }

    private static Set<String> filtrarPorRamo(
        Set<String> atributos, NoOperador ramo
    ) {
        Set<String> resultado = new LinkedHashSet<>();

        for (String atributo : atributos) {
            String tabela = atributo.split("\\.")[0];

            if (contemTabela(ramo, tabela)) {
                resultado.add(atributo);
            }
        }

        return resultado;
    }

    private static boolean estruturaSuportada(NoOperador no) {
        if (no == null) {
            return false;
        }

        switch (no.getTipo()) {
            case TABELA:
                return no.getValor() != null
                    && no.getEsquerdo() == null
                    && no.getDireito() == null;

            case SELECAO:
                return no.getDireito() == null
                    && identificarTabela(no.getValor()) != null
                    && estruturaSuportada(no.getEsquerdo());

            case JUNCAO:
                return juncaoSuportada(no)
                    && estruturaSuportada(no.getEsquerdo())
                    && estruturaSuportada(no.getDireito());

            default:
                return false;
        }
    }

    private static boolean juncaoSuportada(NoOperador no) {
        CondicaoJuncao condicao = lerJuncao(no.getValor());

        if (condicao == null) {
            return false;
        }

        boolean normal = contemTabela(
            no.getEsquerdo(), condicao.tabela1()
        ) && contemTabela(no.getDireito(), condicao.tabela2());

        boolean inversa = contemTabela(
            no.getEsquerdo(), condicao.tabela2()
        ) && contemTabela(no.getDireito(), condicao.tabela1());

        return normal || inversa;
    }

    public static NoOperador priorizarJuncoes(NoOperador raiz) {
        if (raiz == null) {
            return null;
        }

        NoOperador copia = raiz.copiar();
        return reorganizar(copia);
    }

    private static NoOperador reorganizar(NoOperador no) {
        if (no == null) {
            return null;
        }

        if (no.getTipo() == TipoOperador.PROJECAO
                || no.getTipo() == TipoOperador.SELECAO) {
            no.setEsquerdo(reorganizar(no.getEsquerdo()));
            return no;
        }

        if (no.getTipo() != TipoOperador.JUNCAO) {
            return no;
        }

        Map<String, NoOperador> tabelas = new LinkedHashMap<>();
        List<CondicaoJuncao> condicoes = new ArrayList<>();

        if (!coletarJuncoes(no, tabelas, condicoes)
                || tabelas.size() < 3
                || condicoes.size() != tabelas.size() - 1) {
            return no;
        }

        List<String> ordem = new ArrayList<>(tabelas.keySet());

        ordem.sort(Comparator
            .comparingInt((String tabela) ->
                -contarSelecoes(tabelas.get(tabela)))
            .thenComparingInt(tabela ->
                new ArrayList<>(tabelas.keySet()).indexOf(tabela))
        );

        String primeira = ordem.get(0);
        NoOperador resultado = tabelas.get(primeira);

        Set<String> utilizadas = new LinkedHashSet<>();
        utilizadas.add(primeira);

        List<CondicaoJuncao> restantes = new ArrayList<>(condicoes);

        while (utilizadas.size() < tabelas.size()) {
            String proxima = null;
            CondicaoJuncao escolhida = null;

            for (String tabela : ordem) {
                if (utilizadas.contains(tabela)) {
                    continue;
                }

                for (CondicaoJuncao condicao : restantes) {
                    boolean conecta =
                        condicao.tabela1().equalsIgnoreCase(tabela)
                        && contemNome(utilizadas, condicao.tabela2())
                        || condicao.tabela2().equalsIgnoreCase(tabela)
                        && contemNome(utilizadas, condicao.tabela1());

                    if (conecta) {
                        proxima = tabela;
                        escolhida = condicao;
                        break;
                    }
                }

                if (proxima != null) {
                    break;
                }
            }

            if (proxima == null) {
                return no;
            }

            NoOperador juncao = new NoOperador(
                TipoOperador.JUNCAO, escolhida.texto()
            );

            juncao.setEsquerdo(resultado);
            juncao.setDireito(tabelas.get(proxima));

            resultado = juncao;
            utilizadas.add(proxima);
            restantes.remove(escolhida);
        }

        if (!restantes.isEmpty()) {
            return no;
        }

        return resultado;
    }

    private static boolean coletarJuncoes(
        NoOperador no,
        Map<String, NoOperador> tabelas,
        List<CondicaoJuncao> condicoes
    ) {
        if (no == null) {
            return false;
        }

        if (no.getTipo() == TipoOperador.JUNCAO) {
            CondicaoJuncao condicao = lerJuncao(no.getValor());

            if (condicao == null || !juncaoSuportada(no)) {
                return false;
            }

            condicoes.add(condicao);

            return coletarJuncoes(no.getEsquerdo(), tabelas, condicoes)
                && coletarJuncoes(no.getDireito(), tabelas, condicoes);
        }

        if (no.getTipo() == TipoOperador.SELECAO) {
            String tabela = identificarTabela(no.getValor());

            if (tabela == null || no.getDireito() != null
                    || no.getEsquerdo() == null
                    || no.getEsquerdo().getTipo() != TipoOperador.TABELA
                    || !tabela.equalsIgnoreCase(
                        no.getEsquerdo().getValor())) {
                return false;
            }

            return adicionarTabela(no.getEsquerdo().getValor(), no, tabelas);
        }

        if (no.getTipo() == TipoOperador.TABELA) {
            return adicionarTabela(no.getValor(), no, tabelas);
        }

        return false;
    }

    private static boolean adicionarTabela(
        String nome, NoOperador no, Map<String, NoOperador> tabelas
    ) {
        if (nome == null || nome.isBlank()) {
            return false;
        }

        for (String existente : tabelas.keySet()) {
            if (existente.equalsIgnoreCase(nome)) {
                return false;
            }
        }

        tabelas.put(nome, no);
        return true;
    }

    private static CondicaoJuncao lerJuncao(String texto) {
        if (texto == null) {
            return null;
        }

        Matcher matcher = CONDICAO_JUNCAO.matcher(texto);

        if (!matcher.matches()) {
            return null;
        }

        String tabela1 = matcher.group(1).split("\\.")[0];
        String tabela2 = matcher.group(2).split("\\.")[0];

        if (tabela1.equalsIgnoreCase(tabela2)) {
            return null;
        }

        return new CondicaoJuncao(texto, tabela1, tabela2);
    }

    private static int contarSelecoes(NoOperador no) {
        if (no == null) {
            return 0;
        }

        int atual = no.getTipo() == TipoOperador.SELECAO ? 1 : 0;

        return atual + contarSelecoes(no.getEsquerdo())
            + contarSelecoes(no.getDireito());
    }

    private static boolean contemNome(
        Set<String> nomes, String procurado
    ) {
        for (String nome : nomes) {
            if (nome.equalsIgnoreCase(procurado)) {
                return true;
            }
        }

        return false;
    }

    
    private static Set<String> extrairReferencias(String texto) {
        Set<String> referencias = new LinkedHashSet<>();

        if (texto == null) {
            return referencias;
        }

        StringBuilder textoSemLiterais = new StringBuilder();
        boolean dentroString = false;

        for (int i = 0; i < texto.length(); i++) {
            char atual = texto.charAt(i);

            if (atual == '\'') {
                if (dentroString
                        && i + 1 < texto.length()
                        && texto.charAt(i + 1) == '\'') {

                    textoSemLiterais.append("  ");
                    i++;
                    continue;
                }

                dentroString = !dentroString;
                textoSemLiterais.append(' ');
                continue;
            }

            if (dentroString) {
                textoSemLiterais.append(' ');
            } else {
                textoSemLiterais.append(atual);
            }
        }

        Matcher matcher = REFERENCIA.matcher(
                textoSemLiterais.toString()
        );

        while (matcher.find()) {
            referencias.add(matcher.group());
        }

        return referencias;
    }


    private static String identificarTabela(String condicao) {
        if (condicao == null) {
            return null;
        }

        Matcher matcher = CONDICAO_SIMPLES.matcher(condicao);

        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1).split("\\.")[0];
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
