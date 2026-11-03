
package hu05.testes;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.rastreamento.IdentificadorNosAfetados;
import hu05.rastreamento.RastreadorHeuristicas;

import java.util.List;
import java.util.Objects;

public class TestesClassificacaoNos {

    private static int verificacoes = 0;

    public static void main(String[] args) {
        testarSelecaoReposicionada();
        testarProjecoesCriadas();
        testarAusenciaDeFalsasCriacoes();
        testarOperadoresRepetidos();
        testarHistoricoDasEtapas();

        System.out.println();
        System.out.println("==============================");
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println("Verificacoes: " + verificacoes);
        System.out.println("==============================");
    }

    private static void testarSelecaoReposicionada() {
        var resultado = rastrearConsulta();
        var afetados = identificar(resultado);

        var selecoes = afetados.stream()
            .filter(no -> no.etapa() == 3)
            .filter(no -> no.tipo() == TipoOperador.SELECAO)
            .filter(no -> Objects.equals(
                no.valor(), "Cliente.idade > 18"
            ))
            .toList();

        verificar(
            !selecoes.isEmpty(),
            "Selecao antecipada nao foi identificada"
        );

        verificar(
            selecoes.stream().anyMatch(no ->
                no.alteracao() ==
                    IdentificadorNosAfetados.TipoAlteracao
                        .REPOSICIONADO
            ),
            "Selecao deveria ser REPOSICIONADA"
        );

        verificar(
            selecoes.stream().noneMatch(no ->
                no.alteracao() ==
                    IdentificadorNosAfetados.TipoAlteracao
                        .CRIADO
            ),
            "Selecao existente nao deveria ser CRIADA"
        );

        System.out.println(
            "[APROVADO] Selecao reposicionada"
        );
    }

    private static void testarProjecoesCriadas() {
        var resultado = rastrearConsulta();
        var afetados = identificar(resultado);

        verificar(
            resultado.getEtapas().get(4).alterouArvore(),
            "Reducao de atributos deveria alterar a arvore"
        );

        var projecoesCriadas = afetados.stream()
            .filter(no -> no.etapa() == 5)
            .filter(no -> no.tipo() == TipoOperador.PROJECAO)
            .filter(no -> no.alteracao() ==
                IdentificadorNosAfetados.TipoAlteracao
                    .CRIADO)
            .toList();

        verificar(
            !projecoesCriadas.isEmpty(),
            "Reducao de atributos deveria criar projecoes"
        );

        System.out.println(
            "[APROVADO] Projecoes criadas"
        );
    }

    private static void testarAusenciaDeFalsasCriacoes() {
        var resultado = rastrearConsulta();
        var afetados = identificar(resultado);

        for (var afetado : afetados) {
            if (afetado.alteracao() !=
                IdentificadorNosAfetados.TipoAlteracao.CRIADO) {
                continue;
            }

            NoOperador antes =
                resultado.getArvoreAntesDaEtapa(
                    afetado.etapa()
                );

            verificar(
                !contemMesmaOperacao(
                    antes,
                    afetado.tipo(),
                    afetado.valor()
                ),
                "Operacao preexistente classificada como CRIADA: "
                    + afetado.tipo() + " " + afetado.valor()
            );
        }

        System.out.println(
            "[APROVADO] Ausencia de falsas criacoes"
        );
    }

    private static void testarOperadoresRepetidos() {
        NoOperador esquerda = tabela("Cliente");
        NoOperador direita = tabela("Cliente");

        NoOperador produto = new NoOperador(
            TipoOperador.PRODUTO_CARTESIANO,
            "X"
        );

        produto.setEsquerdo(esquerda);
        produto.setDireito(direita);

        var resultado = new RastreadorHeuristicas()
            .rastrear(produto);

        var afetados = identificar(resultado);

        verificar(
            afetados.stream().noneMatch(no ->
                no.tipo() == TipoOperador.TABELA
            ),
            "Tabelas repetidas nao devem receber "
                + "atribuicoes ambiguas"
        );

        System.out.println(
            "[APROVADO] Operadores repetidos"
        );
    }

    private static void testarHistoricoDasEtapas() {
        var resultado = rastrearConsulta();

        verificar(
            resultado.getEtapas().size() == 6,
            "Deveriam existir seis etapas"
        );

        for (int numero = 1; numero <= 6; numero++) {
            NoOperador antes =
                resultado.getArvoreAntesDaEtapa(numero);

            NoOperador depois =
                resultado.getArvoreDepoisDaEtapa(numero);

            verificar(
                antes != null && depois != null,
                "Historico incompleto na etapa " + numero
            );

            verificar(
                resultado.getEtapas().get(numero - 1)
                    .alterouArvore()
                    == !RastreadorHeuristicas.equivalentes(
                        antes, depois
                    ),
                "Indicador de alteracao incorreto"
            );

            if (numero > 1) {
                verificar(
                    RastreadorHeuristicas.equivalentes(
                        resultado.getArvoreDepoisDaEtapa(
                            numero - 1
                        ),
                        antes
                    ),
                    "Historico descontinuo na etapa " + numero
                );
            }
        }

        verificar(
            RastreadorHeuristicas.equivalentes(
                resultado.getArvoreDepoisDaEtapa(6),
                resultado.getArvoreOtimizada()
            ),
            "Arvore final difere da ultima etapa"
        );

        System.out.println(
            "[APROVADO] Historico das etapas"
        );
    }

    private static RastreadorHeuristicas.Resultado
            rastrearConsulta() {

        return new RastreadorHeuristicas()
            .rastrear(criarConsulta());
    }

    private static List<
            IdentificadorNosAfetados.NoAfetado>
            identificar(
                RastreadorHeuristicas.Resultado resultado
            ) {

        return new IdentificadorNosAfetados()
            .identificar(resultado);
    }

    private static NoOperador criarConsulta() {
        NoOperador juncao = new NoOperador(
            TipoOperador.JUNCAO,
            "Cliente.idCliente = Pedido.Cliente_idCliente"
        );

        juncao.setEsquerdo(tabela("Cliente"));
        juncao.setDireito(tabela("Pedido"));

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

    private static boolean contemMesmaOperacao(
            NoOperador no,
            TipoOperador tipo,
            String valor
    ) {
        if (no == null) {
            return false;
        }

        if (no.getTipo() == tipo
                && Objects.equals(no.getValor(), valor)) {
            return true;
        }

        return contemMesmaOperacao(
            no.getEsquerdo(), tipo, valor
        ) || contemMesmaOperacao(
            no.getDireito(), tipo, valor
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
