
package hu05.testes;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.rastreamento.IdentificadorNosAfetados;
import hu05.rastreamento.RastreadorHeuristicas;

import java.util.List;
import java.util.Objects;

public class TestesIdentificadorNosAfetados {

    private static int verificacoes = 0;

    public static void main(String[] args) {
        testarConsultaSemTransformacoes();
        testarConsultaComTransformacoes();
        testarCaminhosDosNos();
        testarResultadoReproduzivel();
        testarEntradaNula();

        System.out.println();
        System.out.println("==============================");
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println("Verificacoes: " + verificacoes);
        System.out.println("==============================");
    }

    private static void testarConsultaSemTransformacoes() {
        NoOperador tabela = tabela("Cliente");

        var resultado = new RastreadorHeuristicas()
            .rastrear(tabela);

        var afetados = new IdentificadorNosAfetados()
            .identificar(resultado);

        verificar(
            resultado.getHeuristicasAplicadas().isEmpty(),
            "Tabela isolada nao deveria ser transformada"
        );

        verificar(
            afetados.isEmpty(),
            "Tabela isolada nao deveria ter nos afetados"
        );

        System.out.println(
            "[APROVADO] Consulta sem transformacoes"
        );
    }

    private static void testarConsultaComTransformacoes() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(criarConsulta());

        var afetados = new IdentificadorNosAfetados()
            .identificar(resultado);

        verificar(
            resultado.getEtapas().size() == 6,
            "O rastreador deveria registrar seis etapas"
        );

        verificar(
            resultado.getEtapas().stream()
                .anyMatch(etapa -> etapa.alterouArvore()),
            "A consulta deveria sofrer alguma transformacao"
        );

        verificar(
            !afetados.isEmpty(),
            "Nenhum no afetado foi identificado"
        );

        for (var afetado : afetados) {
            verificar(
                resultado.getEtapas()
                    .get(afetado.etapa() - 1)
                    .alterouArvore(),
                "No associado a heuristica sem alteracao"
            );
        }

        System.out.println(
            "[APROVADO] Consulta com transformacoes"
        );
    }

    private static void testarCaminhosDosNos() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(criarConsulta());

        var afetados = new IdentificadorNosAfetados()
            .identificar(resultado);

        for (var afetado : afetados) {
            NoOperador arvore =
                resultado.getArvoreDepoisDaEtapa(
                    afetado.etapa()
                );

            NoOperador localizado =
                localizar(arvore, afetado.caminho());

            verificar(
                localizado != null,
                "Caminho inexistente: " + afetado.caminho()
            );

            verificar(
                localizado.getTipo() == afetado.tipo(),
                "Tipo incorreto no caminho informado"
            );

            verificar(
                Objects.equals(
                    localizado.getValor(),
                    afetado.valor()
                ),
                "Valor incorreto no caminho informado"
            );
        }

        System.out.println(
            "[APROVADO] Caminhos dos nos identificados"
        );
    }

    private static void testarResultadoReproduzivel() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(criarConsulta());

        var identificador =
            new IdentificadorNosAfetados();

        var primeira = identificador.identificar(resultado);
        var segunda = identificador.identificar(resultado);

        verificar(
            primeira.equals(segunda),
            "Identificacao produziu resultados diferentes"
        );

        verificar(
            RastreadorHeuristicas.equivalentes(
                resultado.getArvoreOtimizada(),
                resultado.getArvoreDepoisDaEtapa(6)
            ),
            "Ultima etapa difere da arvore otimizada"
        );

        System.out.println(
            "[APROVADO] Resultado reproduzivel"
        );
    }

    private static void testarEntradaNula() {
        boolean rejeitou = false;

        try {
            new IdentificadorNosAfetados()
                .identificar(null);
        } catch (NullPointerException erro) {
            rejeitou = true;
        }

        verificar(
            rejeitou,
            "Resultado nulo deveria ser rejeitado"
        );

        System.out.println(
            "[APROVADO] Entrada nula"
        );
    }

    private static NoOperador criarConsulta() {
        NoOperador cliente = tabela("Cliente");
        NoOperador pedido = tabela("Pedido");

        NoOperador juncao = new NoOperador(
            TipoOperador.JUNCAO,
            "Cliente.idCliente = Pedido.Cliente_idCliente"
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
            "Cliente.nome, Pedido.idPedido"
        );

        projecao.setAtributos(List.of(
            "Cliente.nome",
            "Pedido.idPedido"
        ));

        projecao.setEsquerdo(selecao);

        return projecao;
    }

    private static NoOperador tabela(String nome) {
        return new NoOperador(
            TipoOperador.TABELA,
            nome
        );
    }

    private static NoOperador localizar(
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
