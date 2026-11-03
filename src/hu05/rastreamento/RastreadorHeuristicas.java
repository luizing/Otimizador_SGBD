
package hu05.rastreamento;

import hu04.modelo.NoOperador;
import hu04.otimizacao.DecomposicaoSelecoes;
import hu04.otimizacao.Heuristica;
import hu04.otimizacao.ProdutoCartesianoHeuristica;
import hu04.otimizacao.SimplificadorProjecoes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class RastreadorHeuristicas {

    public record Etapa(
            int numero,
            String heuristica,
            boolean alterouArvore
    ) {}

    private static final class Registro {

        private final NoOperador antes;
        private final NoOperador depois;

        private Registro(
                NoOperador antes,
                NoOperador depois
        ) {
            this.antes = antes.copiar();
            this.depois = depois.copiar();
        }
    }

    public static final class Resultado {

        private final NoOperador arvoreOtimizada;
        private final List<Etapa> etapas;
        private final List<Registro> registros;

        private Resultado(
                NoOperador arvoreOtimizada,
                List<Etapa> etapas,
                List<Registro> registros
        ) {
            this.arvoreOtimizada = arvoreOtimizada.copiar();
            this.etapas = List.copyOf(etapas);
            this.registros = List.copyOf(registros);
        }

        public NoOperador getArvoreOtimizada() {
            return arvoreOtimizada.copiar();
        }

        public List<Etapa> getEtapas() {
            return etapas;
        }

        public NoOperador getArvoreAntesDaEtapa(
                int numero
        ) {
            return obterRegistro(numero).antes.copiar();
        }

        public NoOperador getArvoreDepoisDaEtapa(
                int numero
        ) {
            return obterRegistro(numero).depois.copiar();
        }

        private Registro obterRegistro(int numero) {
            if (numero < 1 || numero > registros.size()) {
                throw new IllegalArgumentException(
                    "Etapa inexistente: " + numero
                );
            }

            return registros.get(numero - 1);
        }

        public List<String> getHeuristicasAplicadas() {
            List<String> aplicadas = new ArrayList<>();

            for (Etapa etapa : etapas) {
                if (etapa.alterouArvore()) {
                    aplicadas.add(etapa.heuristica());
                }
            }

            return List.copyOf(aplicadas);
        }

        public void exibir() {
            System.out.println("=== HEURISTICAS ===");

            for (Etapa etapa : etapas) {
                System.out.printf(
                    "%d. %s - %s%n",
                    etapa.numero(),
                    etapa.heuristica(),
                    etapa.alterouArvore()
                        ? "APLICADA"
                        : "SEM ALTERACAO"
                );
            }
        }
    }

    public Resultado rastrear(NoOperador arvoreOriginal) {
        Objects.requireNonNull(
            arvoreOriginal,
            "A arvore original nao pode ser nula."
        );

        NoOperador atual = arvoreOriginal.copiar();

        List<Etapa> etapas = new ArrayList<>();
        List<Registro> registros = new ArrayList<>();

        atual = executar(
            atual,
            "Eliminacao de produto cartesiano",
            ProdutoCartesianoHeuristica::evitarProdutoCartesiano,
            etapas,
            registros
        );

        atual = executar(
            atual,
            "Decomposicao de selecoes",
            DecomposicaoSelecoes::decompor,
            etapas,
            registros
        );

        atual = executar(
            atual,
            "Antecipacao de selecoes",
            Heuristica::reduzirTuplas,
            etapas,
            registros
        );

        atual = executar(
            atual,
            "Priorizacao de juncoes",
            Heuristica::priorizarJuncoes,
            etapas,
            registros
        );

        atual = executar(
            atual,
            "Reducao de atributos",
            Heuristica::reduzirAtributos,
            etapas,
            registros
        );

        atual = executar(
            atual,
            "Simplificacao de projecoes",
            SimplificadorProjecoes::simplificar,
            etapas,
            registros
        );

        return new Resultado(
            atual,
            etapas,
            registros
        );
    }

    private NoOperador executar(
            NoOperador atual,
            String nome,
            UnaryOperator<NoOperador> transformacao,
            List<Etapa> etapas,
            List<Registro> registros
    ) {
        NoOperador anterior = atual.copiar();

        NoOperador posterior = Objects.requireNonNull(
            transformacao.apply(atual.copiar()),
            "A heuristica retornou uma arvore nula: " + nome
        );

        boolean alterou = !equivalentes(
            anterior,
            posterior
        );

        etapas.add(new Etapa(
            etapas.size() + 1,
            nome,
            alterou
        ));

        registros.add(new Registro(
            anterior,
            posterior
        ));

        return posterior;
    }

    public static boolean equivalentes(
            NoOperador primeiro,
            NoOperador segundo
    ) {
        if (primeiro == segundo) {
            return true;
        }

        if (primeiro == null || segundo == null) {
            return false;
        }

        return primeiro.getTipo() == segundo.getTipo()
            && Objects.equals(
                primeiro.getValor(),
                segundo.getValor()
            )
            && primeiro.getAtributos().equals(
                segundo.getAtributos()
            )
            && equivalentes(
                primeiro.getEsquerdo(),
                segundo.getEsquerdo()
            )
            && equivalentes(
                primeiro.getDireito(),
                segundo.getDireito()
            );
    }
}
