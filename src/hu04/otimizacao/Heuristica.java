
package hu04.otimizacao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Heuristica {

    private static final String IDENT = "[a-zA-Z_]\\w*";

    private static final Pattern REFERENCIA = Pattern.compile(
        "\\b(" + IDENT + ")\\.(" + IDENT + ")\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IDENTIFICADOR = Pattern.compile(
        "\\b" + IDENT + "\\b"
    );

    private static final Pattern OPERADOR = Pattern.compile(
        "<=|>=|<>|=|<|>"
    );

    private record CondicaoJuncao(
        String texto, Set<String> tabelas
    ) {}

    // ---------------------------------------------------------------
    // Heuristica a.i: selecoes reduzem o numero de tuplas
    // ---------------------------------------------------------------

    public static NoOperador reduzirTuplas(NoOperador raiz) {
        if (raiz == null) {
            return null;
        }

        return ordenarSelecoes(otimizarSelecoes(raiz.copiar()));
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

        return descer(no);
    }

    /**
     * Desce a selecao ate o ponto mais baixo em que todas as
     * tabelas citadas na condicao ja estao disponiveis.
     */
    private static NoOperador descer(NoOperador no) {
        NoOperador filho = no.getEsquerdo();

        if (filho == null || no.getDireito() != null
                || !condicaoQualificada(no.getValor())) {
            return no;
        }

        Set<String> tabelas = tabelasDaCondicao(no.getValor());

        if (tabelas.isEmpty()) {
            return no;
        }

        if (filho.getTipo() == TipoOperador.SELECAO) {
            return atravessarSelecao(no, filho);
        }

        if (filho.getTipo() != TipoOperador.JUNCAO) {
            return no;
        }

        boolean esquerda = contemTabelas(filho.getEsquerdo(), tabelas);
        boolean direita = contemTabelas(filho.getDireito(), tabelas);

        if (esquerda == direita) {
            return no;
        }

        if (esquerda) {
            no.setEsquerdo(filho.getEsquerdo());
            filho.setEsquerdo(descer(no));
        } else {
            no.setEsquerdo(filho.getDireito());
            filho.setDireito(descer(no));
        }

        return filho;
    }

    // Uma selecao entre tabelas nao pode impedir a descida de outra.
    private static NoOperador atravessarSelecao(
        NoOperador no, NoOperador selecaoAbaixo
    ) {
        no.setEsquerdo(selecaoAbaixo.getEsquerdo());

        NoOperador resultado = descer(no);

        if (resultado != no) {
            selecaoAbaixo.setEsquerdo(resultado);
            return selecaoAbaixo;
        }

        no.setEsquerdo(selecaoAbaixo);
        return no;
    }

    /**
     * Heuristica b: em selecoes consecutivas, a mais restritiva
     * fica mais perto da tabela e, portanto, executa primeiro.
     */
    private static NoOperador ordenarSelecoes(NoOperador no) {
        if (no == null) {
            return null;
        }

        if (no.getTipo() != TipoOperador.SELECAO) {
            no.setEsquerdo(ordenarSelecoes(no.getEsquerdo()));
            no.setDireito(ordenarSelecoes(no.getDireito()));
            return no;
        }

        List<NoOperador> cadeia = new ArrayList<>();
        NoOperador atual = no;

        while (atual != null
                && atual.getTipo() == TipoOperador.SELECAO) {
            cadeia.add(atual);
            atual = atual.getEsquerdo();
        }

        NoOperador resultado = ordenarSelecoes(atual);

        cadeia.sort(Comparator.comparingInt(
            selecao -> restritividade(selecao.getValor())
        ));

        for (int i = cadeia.size() - 1; i >= 0; i--) {
            NoOperador selecao = cadeia.get(i);
            selecao.setEsquerdo(resultado);
            resultado = selecao;
        }

        return resultado;
    }

    /**
     * Estimativa sem estatisticas do banco: uma igualdade costuma
     * retornar menos tuplas que um intervalo, e um intervalo
     * menos que uma diferenca.
     */
    private static int restritividade(String condicao) {
        if (condicao == null) {
            return 0;
        }

        Matcher matcher = OPERADOR.matcher(removerLiterais(condicao));
        int maior = 0;

        while (matcher.find()) {
            int pontos = switch (matcher.group()) {
                case "=" -> 3;
                case "<>" -> 1;
                default -> 2;
            };

            maior = Math.max(maior, pontos);
        }

        return maior;
    }

    // ---------------------------------------------------------------
    // Heuristica a.ii: projecoes reduzem o numero de atributos
    // ---------------------------------------------------------------

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
            prepararAbaixoDaSelecao(no, necessarios);
            return criarProjecao(no, necessarios);
        }

        if (no.getTipo() == TipoOperador.TABELA) {
            return criarProjecao(no, necessarios);
        }

        return no;
    }

    /**
     * Mantem a ordem das heuristicas: primeiro as selecoes e, na
     * sequencia, a projecao. Abaixo de uma selecao sobre tabela nao
     * ha projecao; acima de uma juncao, os ramos continuam reduzidos.
     */
    private static void prepararAbaixoDaSelecao(
        NoOperador selecao, Set<String> necessarios
    ) {
        Set<String> atributos = new LinkedHashSet<>(necessarios);
        atributos.addAll(extrairReferencias(selecao.getValor()));

        NoOperador filho = selecao.getEsquerdo();

        if (filho.getTipo() == TipoOperador.SELECAO) {
            prepararAbaixoDaSelecao(filho, atributos);
        } else if (filho.getTipo() == TipoOperador.JUNCAO) {
            selecao.setEsquerdo(projetar(filho, atributos));
        }
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
                    && condicaoQualificada(no.getValor())
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

        boolean usaEsquerda = false;
        boolean usaDireita = false;

        for (String tabela : condicao.tabelas()) {
            boolean esquerda = contemTabela(no.getEsquerdo(), tabela);
            boolean direita = contemTabela(no.getDireito(), tabela);

            if (!esquerda && !direita) {
                return false;
            }

            usaEsquerda |= esquerda;
            usaDireita |= direita;
        }

        return usaEsquerda && usaDireita;
    }

    // ---------------------------------------------------------------
    // Heuristica b: juncoes mais restritivas primeiro, sem produto
    // cartesiano entre as tabelas escolhidas
    // ---------------------------------------------------------------

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
            return reorganizarRamos(no);
        }

        NoOperador resultado = construirOrdem(tabelas, condicoes);

        return resultado == null ? reorganizarRamos(no) : resultado;
    }

    // Uma selecao entre tabelas separa a arvore em trechos independentes.
    private static NoOperador reorganizarRamos(NoOperador no) {
        no.setEsquerdo(reorganizar(no.getEsquerdo()));
        no.setDireito(reorganizar(no.getDireito()));
        return no;
    }

    private static NoOperador construirOrdem(
        Map<String, NoOperador> tabelas,
        List<CondicaoJuncao> condicoes
    ) {
        Map<String, Integer> pontos = new HashMap<>();

        for (Map.Entry<String, NoOperador> tabela : tabelas.entrySet()) {
            pontos.put(
                tabela.getKey(),
                pontuarSelecoes(tabela.getValue())
            );
        }

        // A ordenacao e estavel: em caso de empate, vale a ordem da consulta.
        List<String> ordem = new ArrayList<>(tabelas.keySet());
        ordem.sort(Comparator.comparingInt(
            (String tabela) -> -pontos.get(tabela)
        ));

        String primeira = ordem.get(0);
        NoOperador resultado = tabelas.get(primeira);

        Set<String> utilizadas = new HashSet<>();
        utilizadas.add(normalizar(primeira));

        List<CondicaoJuncao> restantes = new ArrayList<>(condicoes);

        while (utilizadas.size() < tabelas.size()) {
            String proxima = null;
            CondicaoJuncao escolhida = null;

            for (String tabela : ordem) {
                if (utilizadas.contains(normalizar(tabela))) {
                    continue;
                }

                CondicaoJuncao condicao = melhorCondicao(
                    tabela, utilizadas, restantes
                );

                if (condicao != null && (proxima == null
                        || pontos.get(tabela) > pontos.get(proxima)
                        || pontos.get(tabela).equals(pontos.get(proxima))
                            && restritividade(condicao.texto())
                                > restritividade(escolhida.texto()))) {
                    proxima = tabela;
                    escolhida = condicao;
                }
            }

            // Sem condicao disponivel, a juncao viraria produto cartesiano.
            if (proxima == null) {
                return null;
            }

            NoOperador juncao = new NoOperador(
                TipoOperador.JUNCAO, escolhida.texto()
            );

            juncao.setEsquerdo(resultado);
            juncao.setDireito(tabelas.get(proxima));

            resultado = juncao;
            utilizadas.add(normalizar(proxima));
            restantes.remove(escolhida);
        }

        return restantes.isEmpty() ? resultado : null;
    }

    private static CondicaoJuncao melhorCondicao(
        String tabela,
        Set<String> utilizadas,
        List<CondicaoJuncao> condicoes
    ) {
        String nome = normalizar(tabela);
        CondicaoJuncao melhor = null;

        for (CondicaoJuncao condicao : condicoes) {
            Set<String> outras = new HashSet<>(condicao.tabelas());

            if (!outras.remove(nome)
                    || outras.isEmpty()
                    || !utilizadas.containsAll(outras)) {
                continue;
            }

            if (melhor == null
                    || restritividade(condicao.texto())
                        > restritividade(melhor.texto())) {
                melhor = condicao;
            }
        }

        return melhor;
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
            NoOperador base = no;

            while (base != null
                    && base.getTipo() == TipoOperador.SELECAO) {
                base = base.getEsquerdo();
            }

            if (base == null || base.getTipo() != TipoOperador.TABELA) {
                return false;
            }

            String nome = normalizar(base.getValor());

            for (NoOperador atual = no; atual != base;
                    atual = atual.getEsquerdo()) {

                Set<String> usadas = tabelasDaCondicao(atual.getValor());

                if (atual.getDireito() != null
                        || !condicaoQualificada(atual.getValor())
                        || usadas.size() != 1
                        || !usadas.contains(nome)) {
                    return false;
                }
            }

            return adicionarTabela(base.getValor(), no, tabelas);
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
        if (texto == null || !condicaoQualificada(texto)) {
            return null;
        }

        Set<String> tabelas = tabelasDaCondicao(texto);

        if (tabelas.size() < 2) {
            return null;
        }

        return new CondicaoJuncao(texto, tabelas);
    }

    private static int pontuarSelecoes(NoOperador no) {
        if (no == null) {
            return 0;
        }

        int atual = no.getTipo() == TipoOperador.SELECAO
            ? restritividade(no.getValor())
            : 0;

        return atual + pontuarSelecoes(no.getEsquerdo())
            + pontuarSelecoes(no.getDireito());
    }

    // ---------------------------------------------------------------
    // Leitura das condicoes
    // ---------------------------------------------------------------

    private static Set<String> extrairReferencias(String texto) {
        Set<String> referencias = new LinkedHashSet<>();

        if (texto == null) {
            return referencias;
        }

        Matcher matcher = REFERENCIA.matcher(removerLiterais(texto));

        while (matcher.find()) {
            referencias.add(matcher.group());
        }

        return referencias;
    }

    private static Set<String> tabelasDaCondicao(String condicao) {
        Set<String> tabelas = new LinkedHashSet<>();

        for (String referencia : extrairReferencias(condicao)) {
            tabelas.add(normalizar(referencia.split("\\.")[0]));
        }

        return tabelas;
    }

    /**
     * Uma coluna sem tabela nao pode ser posicionada com seguranca.
     * Consultas vindas do parser ja chegam aqui qualificadas.
     */
    private static boolean condicaoQualificada(String condicao) {
        if (condicao == null) {
            return false;
        }

        String semReferencias = REFERENCIA
            .matcher(removerLiterais(condicao))
            .replaceAll(" ");

        Matcher matcher = IDENTIFICADOR.matcher(semReferencias);

        while (matcher.find()) {
            if (!matcher.group().equalsIgnoreCase("AND")) {
                return false;
            }
        }

        return true;
    }

    private static String removerLiterais(String texto) {
        StringBuilder resultado = new StringBuilder();
        boolean dentroString = false;

        for (int i = 0; i < texto.length(); i++) {
            char atual = texto.charAt(i);

            if (atual == '\'') {
                if (dentroString
                        && i + 1 < texto.length()
                        && texto.charAt(i + 1) == '\'') {

                    resultado.append("  ");
                    i++;
                    continue;
                }

                dentroString = !dentroString;
                resultado.append(' ');
                continue;
            }

            resultado.append(dentroString ? ' ' : atual);
        }

        return resultado.toString();
    }

    private static String normalizar(String nome) {
        return nome.toLowerCase(Locale.ROOT);
    }

    private static boolean contemTabelas(
        NoOperador no, Set<String> tabelas
    ) {
        for (String tabela : tabelas) {
            if (!contemTabela(no, tabela)) {
                return false;
            }
        }

        return true;
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
