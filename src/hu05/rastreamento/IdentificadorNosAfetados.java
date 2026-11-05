
package hu05.rastreamento;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IdentificadorNosAfetados {

    public enum TipoAlteracao {
        CRIADO,
        MODIFICADO,
        REPOSICIONADO
    }

    public record NoAfetado(
            int etapa,
            String heuristica,
            TipoAlteracao alteracao,
            String caminho,
            TipoOperador tipo,
            String valor
    ) {}

    private record Ocorrencia(
            NoOperador no,
            String caminho,
            String assinatura,
            String assinaturaPai
    ) {}

    public List<NoAfetado> identificar(
            RastreadorHeuristicas.Resultado resultado
    ) {
        Objects.requireNonNull(
            resultado,
            "O resultado do rastreamento nao pode ser nulo."
        );

        List<NoAfetado> afetados = new ArrayList<>();

        for (RastreadorHeuristicas.Etapa etapa
                : resultado.getEtapas()) {

            if (!etapa.alterouArvore()) {
                continue;
            }

            NoOperador antes =
                resultado.getArvoreAntesDaEtapa(
                    etapa.numero()
                );

            NoOperador depois =
                resultado.getArvoreDepoisDaEtapa(
                    etapa.numero()
                );

            afetados.addAll(
                comparar(
                    etapa.numero(),
                    etapa.heuristica(),
                    antes,
                    depois
                )
            );
        }

        return List.copyOf(afetados);
    }

    private List<NoAfetado> comparar(
            int etapa,
            String heuristica,
            NoOperador antes,
            NoOperador depois
    ) {
        List<Ocorrencia> anteriores = new ArrayList<>();
        List<Ocorrencia> posteriores = new ArrayList<>();

        coletar(
            antes, "R", null, anteriores
        );

        coletar(
            depois, "R", null, posteriores
        );

        Map<String, List<Ocorrencia>> porAssinaturaAntes =
            agruparPorAssinatura(anteriores);

        Map<String, List<Ocorrencia>> porAssinaturaDepois =
            agruparPorAssinatura(posteriores);

        Map<String, Ocorrencia> porCaminhoAntes =
            agruparPorCaminho(anteriores);

        List<NoAfetado> encontrados = new ArrayList<>();

        for (Ocorrencia atual : posteriores) {
            List<Ocorrencia> correspondentesAntes =
                porAssinaturaAntes.getOrDefault(
                    atual.assinatura(),
                    List.of()
                );

            List<Ocorrencia> correspondentesDepois =
                porAssinaturaDepois.getOrDefault(
                    atual.assinatura(),
                    List.of()
                );

            // Duas projecoes iguais foram unidas: a que restou e a afetada.
            if (RastreadorHeuristicas.SIMPLIFICACAO.equals(heuristica)
                    && atual.no().getTipo() == TipoOperador.PROJECAO
                    && correspondentesAntes.size() > 1
                    && correspondentesDepois.size() == 1) {

                encontrados.add(criarRegistro(
                    etapa,
                    heuristica,
                    TipoAlteracao.MODIFICADO,
                    atual
                ));

                continue;
            }

            // Assinaturas repetidas tornam a identidade ambigua.
            if (correspondentesAntes.size() > 1
                    || correspondentesDepois.size() > 1) {
                continue;
            }

            if (correspondentesAntes.size() == 1) {
                Ocorrencia anterior =
                    correspondentesAntes.get(0);

                // A mudanca de pai e uma evidencia mais
                // especifica que a simples mudanca de caminho.
                boolean paiMudou = !Objects.equals(
                    anterior.assinaturaPai(),
                    atual.assinaturaPai()
                );

                boolean filhosMudaram = !filhosCorrespondem(
                    anterior.no(),
                    atual.no()
                );

                TipoAlteracao alteracao = classificarMovimento(
                    heuristica,
                    atual.no().getTipo(),
                    paiMudou,
                    filhosMudaram
                );

                if (alteracao != null) {
                    encontrados.add(criarRegistro(
                        etapa,
                        heuristica,
                        alteracao,
                        atual
                    ));
                }

                continue;
            }

            // Um operador novo so conta para a heuristica que produz
            // esse tipo de operador.
            if (!tipoAlterado(heuristica, atual.no().getTipo())) {
                continue;
            }

            Ocorrencia mesmaPosicao =
                porCaminhoAntes.get(atual.caminho());

            if (mesmaPosicao != null
                    && mesmaPosicao.no().getTipo()
                        == atual.no().getTipo()
                    && filhosCorrespondem(
                        mesmaPosicao.no(),
                        atual.no()
                    )) {

                encontrados.add(criarRegistro(
                    etapa,
                    heuristica,
                    TipoAlteracao.MODIFICADO,
                    atual
                ));

            } else {
                // O operador nao existia com esta assinatura
                // no estado anterior.
                encontrados.add(criarRegistro(
                    etapa,
                    heuristica,
                    TipoAlteracao.CRIADO,
                    atual
                ));
            }
        }

        return encontrados;
    }

    /**
     * Um no tambem muda de pai quando apenas o operador vizinho foi movido.
     * Por isso, o movimento so conta para a heuristica que realmente
     * desloca esse tipo de operador.
     */
    private TipoAlteracao classificarMovimento(
            String heuristica,
            TipoOperador tipo,
            boolean paiMudou,
            boolean filhosMudaram
    ) {
        boolean movido = switch (heuristica) {
            case RastreadorHeuristicas.ANTECIPACAO ->
                tipo == TipoOperador.SELECAO && paiMudou;

            case RastreadorHeuristicas.PRIORIZACAO ->
                tipo == TipoOperador.JUNCAO
                    && (paiMudou || filhosMudaram);

            default -> false;
        };

        if (movido) {
            return TipoAlteracao.REPOSICIONADO;
        }

        return RastreadorHeuristicas.SIMPLIFICACAO.equals(heuristica)
                && tipo == TipoOperador.PROJECAO
                && filhosMudaram
            ? TipoAlteracao.MODIFICADO
            : null;
    }

    private boolean tipoAlterado(
            String heuristica,
            TipoOperador tipo
    ) {
        return switch (heuristica) {
            case RastreadorHeuristicas.ELIMINACAO_PRODUTO,
                 RastreadorHeuristicas.PRIORIZACAO ->
                tipo == TipoOperador.JUNCAO;

            case RastreadorHeuristicas.DECOMPOSICAO,
                 RastreadorHeuristicas.ANTECIPACAO ->
                tipo == TipoOperador.SELECAO;

            case RastreadorHeuristicas.REDUCAO_ATRIBUTOS,
                 RastreadorHeuristicas.SIMPLIFICACAO ->
                tipo == TipoOperador.PROJECAO;

            default -> true;
        };
    }

    private boolean filhosCorrespondem(
            NoOperador primeiro,
            NoOperador segundo
    ) {
        return assinaturaOuNulo(
                primeiro.getEsquerdo()
            ).equals(
                assinaturaOuNulo(
                    segundo.getEsquerdo()
                )
            )
            && assinaturaOuNulo(
                primeiro.getDireito()
            ).equals(
                assinaturaOuNulo(
                    segundo.getDireito()
                )
            );
    }

    private String assinaturaOuNulo(NoOperador no) {
        return no == null
            ? "<NULO>"
            : assinatura(no);
    }

    private NoAfetado criarRegistro(
            int etapa,
            String heuristica,
            TipoAlteracao alteracao,
            Ocorrencia ocorrencia
    ) {
        return new NoAfetado(
            etapa,
            heuristica,
            alteracao,
            ocorrencia.caminho(),
            ocorrencia.no().getTipo(),
            ocorrencia.no().getValor()
        );
    }

    private void coletar(
            NoOperador no,
            String caminho,
            String assinaturaPai,
            List<Ocorrencia> destino
    ) {
        if (no == null) {
            return;
        }

        String assinaturaAtual = assinatura(no);

        destino.add(new Ocorrencia(
            no,
            caminho,
            assinaturaAtual,
            assinaturaPai
        ));

        coletar(
            no.getEsquerdo(),
            caminho + "E",
            assinaturaAtual,
            destino
        );

        coletar(
            no.getDireito(),
            caminho + "D",
            assinaturaAtual,
            destino
        );
    }

    private String assinatura(NoOperador no) {
        // Representacao inequivoca dos campos, sem depender
        // de separadores que possam aparecer no SQL.
        return no.getTipo().name()
            + codificar(no.getValor())
            + codificarLista(no.getAtributos());
    }

    private String codificar(String valor) {
        if (valor == null) {
            return "N;";
        }

        return "S" + valor.length() + ":" + valor;
    }

    private String codificarLista(List<String> valores) {
        StringBuilder resultado = new StringBuilder();

        resultado.append("L").append(valores.size());

        for (String valor : valores) {
            resultado.append(codificar(valor));
        }

        return resultado.toString();
    }

    private Map<String, List<Ocorrencia>> agruparPorAssinatura(
            List<Ocorrencia> ocorrencias
    ) {
        Map<String, List<Ocorrencia>> mapa =
            new HashMap<>();

        for (Ocorrencia ocorrencia : ocorrencias) {
            mapa.computeIfAbsent(
                ocorrencia.assinatura(),
                chave -> new ArrayList<>()
            ).add(ocorrencia);
        }

        return mapa;
    }

    private Map<String, Ocorrencia> agruparPorCaminho(
            List<Ocorrencia> ocorrencias
    ) {
        Map<String, Ocorrencia> mapa =
            new HashMap<>();

        for (Ocorrencia ocorrencia : ocorrencias) {
            mapa.put(
                ocorrencia.caminho(),
                ocorrencia
            );
        }

        return mapa;
    }
}
