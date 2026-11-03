
package hu05.servico;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.modelo.PassoExecucao;
import hu05.modelo.PlanoExecucao;

import hu05.rastreamento.IdentificadorNosAfetados;
import hu05.rastreamento.RastreadorHeuristicas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GeradorPlanoExecucao {

    private record Assinatura(
            TipoOperador tipo,
            String valor,
            List<String> atributos
    ) {}

    /**
     * Mantem compatibilidade com a implementacao anterior.
     * Gera o plano sem informacoes de heuristicas.
     */
    public PlanoExecucao gerar(NoOperador arvoreOtimizada) {
        if (arvoreOtimizada == null) {
            throw new IllegalArgumentException(
                "A arvore otimizada nao pode ser nula."
            );
        }

        return construirPlano(
            arvoreOtimizada,
            new IdentityHashMap<>()
        );
    }

    /**
     * Gera o plano a partir do resultado do rastreamento.
     * Associa apenas heuristicas com correspondencia
     * unica na arvore final.
     */
    public PlanoExecucao gerarComHeuristicas(
        RastreadorHeuristicas.Resultado resultado
    ) {
        Objects.requireNonNull(
            resultado,
            "O resultado do rastreamento nao pode ser nulo."
        );

        NoOperador arvoreFinal =
            resultado.getArvoreOtimizada();

        List<IdentificadorNosAfetados.NoAfetado> afetados =
            new IdentificadorNosAfetados()
                .identificar(resultado);

        Map<Assinatura, Integer> contagemFinal =
            contarAssinaturas(arvoreFinal);

        Map<Assinatura, List<String>> associacoes =
            new HashMap<>();

        for (IdentificadorNosAfetados.NoAfetado afetado
                : afetados) {

            NoOperador arvoreDaEtapa =
                resultado.getArvoreDepoisDaEtapa(
                    afetado.etapa()
                );

            NoOperador noDaEtapa = localizar(
                arvoreDaEtapa,
                afetado.caminho()
            );

            if (noDaEtapa == null) {
                continue;
            }

            Assinatura assinatura =
                assinatura(noDaEtapa);

            // Evita associar uma heuristica a operadores
            // indistinguiveis dentro da mesma etapa.
            Map<Assinatura, Integer> contagemEtapa =
                contarAssinaturas(arvoreDaEtapa);

            if (contagemEtapa.getOrDefault(
                    assinatura, 0) != 1) {
                continue;
            }

            // A operacao precisa existir uma unica vez
            // na arvore otimizada final.
            if (contagemFinal.getOrDefault(
                    assinatura, 0) != 1) {
                continue;
            }

            List<String> heuristicas =
                associacoes.computeIfAbsent(
                    assinatura,
                    chave -> new ArrayList<>()
                );

            if (!heuristicas.contains(
                    afetado.heuristica()
                )) {
                heuristicas.add(
                    afetado.heuristica()
                );
            }
        }

        IdentityHashMap<NoOperador, List<String>>
            heuristicasPorNo = new IdentityHashMap<>();

        associarNosFinais(
            arvoreFinal,
            associacoes,
            heuristicasPorNo
        );

        return construirPlano(
            arvoreFinal,
            heuristicasPorNo
        );
    }

    private PlanoExecucao construirPlano(
            NoOperador raiz,
            IdentityHashMap<NoOperador, List<String>>
                heuristicasPorNo
    ) {
        List<PassoExecucao> passos = new ArrayList<>();

        percorrer(
            raiz,
            passos,
            heuristicasPorNo
        );

        return new PlanoExecucao(passos);
    }

    /**
     * Percurso em pos-ordem:
     * filho esquerdo, filho direito, operador atual.
     */
    private int percorrer(
            NoOperador no,
            List<PassoExecucao> passos,
            IdentityHashMap<NoOperador, List<String>>
                heuristicasPorNo
    ) {
        if (no == null) {
            return 0;
        }

        List<Integer> dependencias =
            new ArrayList<>();

        if (no.getEsquerdo() != null) {
            dependencias.add(
                percorrer(
                    no.getEsquerdo(),
                    passos,
                    heuristicasPorNo
                )
            );
        }

        if (no.getDireito() != null) {
            dependencias.add(
                percorrer(
                    no.getDireito(),
                    passos,
                    heuristicasPorNo
                )
            );
        }

        int numero = passos.size() + 1;

        List<String> heuristicas =
            heuristicasPorNo.getOrDefault(
                no,
                List.of()
            );

        PassoExecucao passo = new PassoExecucao(
            numero,
            no.getTipo(),
            criarDescricao(no),
            dependencias,
            heuristicas
        );

        passos.add(passo);

        return numero;
    }

    private void associarNosFinais(
            NoOperador no,
            Map<Assinatura, List<String>> associacoes,
            IdentityHashMap<NoOperador, List<String>>
                heuristicasPorNo
    ) {
        if (no == null) {
            return;
        }

        List<String> heuristicas =
            associacoes.get(assinatura(no));

        if (heuristicas != null
                && !heuristicas.isEmpty()) {

            heuristicasPorNo.put(
                no,
                List.copyOf(heuristicas)
            );
        }

        associarNosFinais(
            no.getEsquerdo(),
            associacoes,
            heuristicasPorNo
        );

        associarNosFinais(
            no.getDireito(),
            associacoes,
            heuristicasPorNo
        );
    }

    private Map<Assinatura, Integer> contarAssinaturas(
            NoOperador raiz
    ) {
        Map<Assinatura, Integer> contagem =
            new HashMap<>();

        contarRecursivamente(
            raiz,
            contagem
        );

        return contagem;
    }

    private void contarRecursivamente(
            NoOperador no,
            Map<Assinatura, Integer> contagem
    ) {
        if (no == null) {
            return;
        }

        Assinatura chave = assinatura(no);

        contagem.merge(
            chave,
            1,
            Integer::sum
        );

        contarRecursivamente(
            no.getEsquerdo(),
            contagem
        );

        contarRecursivamente(
            no.getDireito(),
            contagem
        );
    }

    private Assinatura assinatura(NoOperador no) {
        return new Assinatura(
            no.getTipo(),
            no.getValor(),
            List.copyOf(no.getAtributos())
        );
    }

    /**
     * Localiza um no pelo caminho registrado na
     * arvore intermediaria.
     *
     * R  = raiz
     * E  = filho esquerdo
     * D  = filho direito
     */
    private NoOperador localizar(
            NoOperador raiz,
            String caminho
    ) {
        if (raiz == null
                || caminho == null
                || !caminho.startsWith("R")) {
            return null;
        }

        NoOperador atual = raiz;

        for (int i = 1; i < caminho.length(); i++) {
            if (atual == null) {
                return null;
            }

            char direcao = caminho.charAt(i);

            if (direcao == 'E') {
                atual = atual.getEsquerdo();
            } else if (direcao == 'D') {
                atual = atual.getDireito();
            } else {
                return null;
            }
        }

        return atual;
    }

    private String criarDescricao(NoOperador no) {
        String valor = no.getValor() == null
            ? ""
            : no.getValor();

        TipoOperador tipo = no.getTipo();

        switch (tipo) {
            case TABELA:
                return "Ler tabela: " + valor;

            case SELECAO:
                return "Aplicar selecao: " + valor;

            case PROJECAO:
                return "Projetar atributos: " + valor;

            case JUNCAO:
                return "Executar juncao: " + valor;

            case PRODUTO_CARTESIANO:
                return "Executar produto cartesiano: " + valor;

            default:
                throw new IllegalStateException(
                    "Operador nao suportado: " + tipo
                );
        }
    }
}
