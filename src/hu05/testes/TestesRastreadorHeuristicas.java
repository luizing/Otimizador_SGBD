
package hu05.testes;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;
import hu04.otimizacao.OtimizarConsulta;

import hu05.rastreamento.RastreadorHeuristicas;

import java.util.List;

public class TestesRastreadorHeuristicas {

    private static int verificacoes = 0;

    public static void main(String[] args) {
        testarEquivalenciaComHU04();
        testarPreservacaoOriginal();
        testarEtapas();
        testarIndependenciaDoResultado();

        System.out.println();
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println(
            "Verificacoes: " + verificacoes
        );
    }

    private static NoOperador criarArvore() {
        NoOperador cliente = new NoOperador(
            TipoOperador.TABELA, "Cliente"
        );

        NoOperador pedido = new NoOperador(
            TipoOperador.TABELA, "pedido"
        );

        NoOperador juncao = new NoOperador(
            TipoOperador.JUNCAO,
            "Cliente.idCliente = pedido.Cliente_idCliente"
        );

        juncao.setEsquerdo(cliente);
        juncao.setDireito(pedido);

        NoOperador selecao = new NoOperador(
            TipoOperador.SELECAO,
            "Cliente.idade > 18"
        );

        selecao.setEsquerdo(juncao);

        NoOperador projecao = new NoOperador(
            TipoOperador.PROJECAO,
            "Cliente.nome, pedido.idPedido"
        );

        projecao.setAtributos(List.of(
            "Cliente.nome",
            "pedido.idPedido"
        ));

        projecao.setEsquerdo(selecao);

        return projecao;
    }

    private static void testarEquivalenciaComHU04() {
        NoOperador original = criarArvore();

        NoOperador esperado =
            new OtimizarConsulta().otimizar(original);

        NoOperador obtido =
            new RastreadorHeuristicas()
                .rastrear(original)
                .getArvoreOtimizada();

        verificar(
            RastreadorHeuristicas.equivalentes(
                esperado, obtido
            ),
            "Resultado diferente do otimizador HU04"
        );

        System.out.println(
            "[APROVADO] Equivalencia com HU04"
        );
    }

    private static void testarPreservacaoOriginal() {
        NoOperador original = criarArvore();
        NoOperador copia = original.copiar();

        new RastreadorHeuristicas().rastrear(original);

        verificar(
            RastreadorHeuristicas.equivalentes(
                original, copia
            ),
            "Arvore original foi modificada"
        );

        System.out.println(
            "[APROVADO] Preservacao da arvore original"
        );
    }

    private static void testarEtapas() {
        var resultado =
            new RastreadorHeuristicas()
                .rastrear(criarArvore());

        verificar(
            resultado.getEtapas().size() == 6,
            "Quantidade incorreta de etapas"
        );

        for (int i = 0; i < 6; i++) {
            verificar(
                resultado.getEtapas().get(i).numero() == i + 1,
                "Numeracao incorreta"
            );
        }

        verificar(
            resultado.getEtapas().stream()
                .anyMatch(etapa ->
                    etapa.heuristica().equals(
                        "Antecipacao de selecoes"
                    ) && etapa.alterouArvore()
                ),
            "Antecipacao de selecoes nao detectada"
        );

        System.out.println(
            "[APROVADO] Registro das heuristicas"
        );

        resultado.exibir();
    }

    private static void testarIndependenciaDoResultado() {
        var resultado =
            new RastreadorHeuristicas()
                .rastrear(criarArvore());

        NoOperador primeira =
            resultado.getArvoreOtimizada();

        NoOperador segunda =
            resultado.getArvoreOtimizada();

        primeira.setValor("ALTERADO");

        verificar(
            !RastreadorHeuristicas.equivalentes(
                primeira, segunda
            ),
            "Resultado expôs a arvore interna"
        );

        System.out.println(
            "[APROVADO] Independencia do resultado"
        );
    }

    private static void verificar(
            boolean condicao,
            String mensagem
    ) {
        verificacoes++;

        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
