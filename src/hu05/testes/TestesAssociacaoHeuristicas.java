
package hu05.testes;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.modelo.PassoExecucao;
import hu05.modelo.PlanoExecucao;
import hu05.rastreamento.RastreadorHeuristicas;
import hu05.servico.GeradorPlanoExecucao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestesAssociacaoHeuristicas {

    private static int verificacoes = 0;

    public static void main(String[] args) {
        testarConsultaSemHeuristicas();
        testarAssociacaoDaSelecao();
        testarOrdemEDependencias();
        testarCompatibilidade();
        testarEntradaNula();

        System.out.println();
        System.out.println("==============================");
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println("Verificacoes: " + verificacoes);
        System.out.println("==============================");
    }

    private static void testarConsultaSemHeuristicas() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(tabela("Cliente"));

        PlanoExecucao plano = new GeradorPlanoExecucao()
            .gerarComHeuristicas(resultado);

        verificar(
            plano.getQuantidadePassos() == 1,
            "Tabela isolada deve gerar um passo"
        );

        verificar(
            plano.getPassos().get(0).getHeuristicas().isEmpty(),
            "Tabela isolada nao deve receber heuristicas"
        );

        System.out.println(
            "[APROVADO] Consulta sem heuristicas"
        );
    }

    private static void testarAssociacaoDaSelecao() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(criarConsulta());

        PlanoExecucao plano = new GeradorPlanoExecucao()
            .gerarComHeuristicas(resultado);

        verificar(
            resultado.getHeuristicasAplicadas()
                .contains("Antecipacao de selecoes"),
            "Rastreador nao detectou antecipacao"
        );

        List<PassoExecucao> selecoes = plano.getPassos()
            .stream()
            .filter(passo ->
                passo.getTipo() == TipoOperador.SELECAO
            )
            .filter(passo ->
                passo.getDescricao()
                    .contains("Cliente.idade > 18")
            )
            .toList();

        verificar(
            selecoes.size() == 1,
            "Deveria existir uma selecao identificavel"
        );

        verificar(
            selecoes.get(0).getHeuristicas()
                .contains("Antecipacao de selecoes"),
            "Selecao nao recebeu a heuristica esperada"
        );

        System.out.println(
            "[APROVADO] Associacao da selecao"
        );
    }

    private static void testarOrdemEDependencias() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(criarConsulta());

        PlanoExecucao plano = new GeradorPlanoExecucao()
            .gerarComHeuristicas(resultado);

        Set<Integer> executados = new HashSet<>();

        for (PassoExecucao passo : plano.getPassos()) {
            verificar(
                passo.getNumero() == executados.size() + 1,
                "Numeracao incorreta"
            );

            for (Integer dependencia :
                    passo.getDependencias()) {
                verificar(
                    executados.contains(dependencia),
                    "Dependencia executada fora de ordem"
                );
            }

            executados.add(passo.getNumero());
        }

        verificar(
            plano.getPassos()
                .get(plano.getQuantidadePassos() - 1)
                .getTipo() == TipoOperador.PROJECAO,
            "Projecao final deveria ser a ultima operacao"
        );

        System.out.println(
            "[APROVADO] Ordem e dependencias"
        );
    }

    private static void testarCompatibilidade() {
        var resultado = new RastreadorHeuristicas()
            .rastrear(criarConsulta());

        GeradorPlanoExecucao gerador =
            new GeradorPlanoExecucao();

        PlanoExecucao tradicional = gerador.gerar(
            resultado.getArvoreOtimizada()
        );

        PlanoExecucao anotado = gerador
            .gerarComHeuristicas(resultado);

        verificar(
            tradicional.getQuantidadePassos()
                == anotado.getQuantidadePassos(),
            "Planos possuem quantidades diferentes"
        );

        for (int i = 0;
                i < tradicional.getQuantidadePassos();
                i++) {

            PassoExecucao primeiro =
                tradicional.getPassos().get(i);

            PassoExecucao segundo =
                anotado.getPassos().get(i);

            verificar(
                primeiro.getTipo() == segundo.getTipo(),
                "Tipo de operador alterado"
            );

            verificar(
                primeiro.getDescricao()
                    .equals(segundo.getDescricao()),
                "Descricao de operador alterada"
            );

            verificar(
                primeiro.getDependencias()
                    .equals(segundo.getDependencias()),
                "Dependencias foram alteradas"
            );

            verificar(
                primeiro.getHeuristicas().isEmpty(),
                "Metodo tradicional nao deve anotar heuristicas"
            );
        }

        System.out.println(
            "[APROVADO] Compatibilidade dos metodos"
        );
    }

    private static void testarEntradaNula() {
        boolean rejeitou = false;

        try {
            new GeradorPlanoExecucao()
                .gerarComHeuristicas(null);
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
