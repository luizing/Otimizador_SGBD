
package hu05.testes;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.modelo.PassoExecucao;
import hu05.modelo.PlanoExecucao;
import hu05.servico.GeradorPlanoExecucao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestesPlanoExecucao {

    private static int verificacoes = 0;

    public static void main(String[] args) {
        testarTabelaIsolada();
        testarSelecaoEProjecao();
        testarJuncao();
        testarPlanoCompleto();
        testarArvoreNula();
        testarArvorePreservada();

        System.out.println();
        System.out.println("==============================");
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println("Verificacoes: " + verificacoes);
        System.out.println("==============================");
    }

    private static void testarTabelaIsolada() {
        NoOperador tabela = tabela("Cliente");

        PlanoExecucao plano =
            new GeradorPlanoExecucao().gerar(tabela);

        verificar(plano.getQuantidadePassos() == 1,
            "Tabela isolada deve gerar um passo");

        verificar(plano.getPassos().get(0).getTipo()
            == TipoOperador.TABELA,
            "Primeiro passo deve ser tabela");

        verificar(plano.getPassos().get(0)
            .getDependencias().isEmpty(),
            "Leitura de tabela nao deve ter dependencias");

        System.out.println("[APROVADO] Tabela isolada");
    }

    private static void testarSelecaoEProjecao() {
        NoOperador tabela = tabela("Cliente");

        NoOperador selecao = no(
            TipoOperador.SELECAO,
            "Cliente.idade > 18",
            tabela,
            null
        );

        NoOperador projecao = no(
            TipoOperador.PROJECAO,
            "Cliente.nome",
            selecao,
            null
        );

        PlanoExecucao plano =
            new GeradorPlanoExecucao().gerar(projecao);

        List<PassoExecucao> passos = plano.getPassos();

        verificar(passos.size() == 3,
            "Deveria haver tres passos");

        verificar(passos.get(0).getTipo()
            == TipoOperador.TABELA,
            "Tabela deve aparecer primeiro");

        verificar(passos.get(1).getTipo()
            == TipoOperador.SELECAO,
            "Selecao deve aparecer depois da tabela");

        verificar(passos.get(2).getTipo()
            == TipoOperador.PROJECAO,
            "Projecao deve aparecer por ultimo");

        verificar(passos.get(1).getDependencias()
            .equals(List.of(1)),
            "Selecao deve depender da tabela");

        verificar(passos.get(2).getDependencias()
            .equals(List.of(2)),
            "Projecao deve depender da selecao");

        System.out.println("[APROVADO] Selecao e projecao");
    }

    private static void testarJuncao() {
        NoOperador juncao = no(
            TipoOperador.JUNCAO,
            "Cliente.idCliente = Pedido.Cliente_idCliente",
            tabela("Cliente"),
            tabela("Pedido")
        );

        PlanoExecucao plano =
            new GeradorPlanoExecucao().gerar(juncao);

        List<PassoExecucao> passos = plano.getPassos();

        verificar(passos.size() == 3,
            "Juncao deve gerar tres passos");

        verificar(passos.get(0).getTipo()
            == TipoOperador.TABELA,
            "Ramo esquerdo deve aparecer primeiro");

        verificar(passos.get(1).getTipo()
            == TipoOperador.TABELA,
            "Ramo direito deve aparecer em seguida");

        verificar(passos.get(2).getTipo()
            == TipoOperador.JUNCAO,
            "Juncao deve aparecer depois das tabelas");

        verificar(passos.get(2).getDependencias()
            .equals(List.of(1, 2)),
            "Juncao deve depender dos dois ramos");

        System.out.println("[APROVADO] Juncao");
    }

    private static void testarPlanoCompleto() {
        NoOperador selecao = no(
            TipoOperador.SELECAO,
            "Cliente.idade > 18",
            tabela("Cliente"),
            null
        );

        NoOperador juncao = no(
            TipoOperador.JUNCAO,
            "Cliente.idCliente = Pedido.Cliente_idCliente",
            selecao,
            tabela("Pedido")
        );

        NoOperador projecao = no(
            TipoOperador.PROJECAO,
            "Cliente.nome, Pedido.idPedido",
            juncao,
            null
        );

        PlanoExecucao plano =
            new GeradorPlanoExecucao().gerar(projecao);

        List<PassoExecucao> passos = plano.getPassos();

        verificar(passos.size() == 5,
            "Plano completo deve ter cinco passos");

        verificar(passos.get(4).getTipo()
            == TipoOperador.PROJECAO,
            "Ultimo passo deve ser a projecao");

        verificar(passos.get(3).getDependencias()
            .equals(List.of(2, 3)),
            "Juncao deve depender da selecao e de Pedido");

        verificar(passos.get(4).getDependencias()
            .equals(List.of(4)),
            "Projecao final deve depender da juncao");

        Set<Integer> executados = new HashSet<>();

        for (PassoExecucao passo : passos) {
            verificar(
                passo.getNumero() == executados.size() + 1,
                "Numeracao incorreta"
            );

            for (int dependencia : passo.getDependencias()) {
                verificar(
                    executados.contains(dependencia),
                    "Dependencia executada fora de ordem"
                );
            }

            executados.add(passo.getNumero());
        }

        System.out.println("[APROVADO] Plano completo");
    }

    private static void testarArvoreNula() {
        boolean excecao = false;

        try {
            new GeradorPlanoExecucao().gerar(null);
        } catch (IllegalArgumentException erro) {
            excecao = true;
        }

        verificar(excecao,
            "Arvore nula deve ser rejeitada");

        System.out.println("[APROVADO] Arvore nula");
    }

    private static void testarArvorePreservada() {
        NoOperador tabela = tabela("Cliente");

        NoOperador selecao = no(
            TipoOperador.SELECAO,
            "Cliente.idade > 18",
            tabela,
            null
        );

        new GeradorPlanoExecucao().gerar(selecao);

        verificar(selecao.getEsquerdo() == tabela,
            "Gerador modificou a arvore");

        verificar(selecao.getValor()
            .equals("Cliente.idade > 18"),
            "Gerador modificou a condicao");

        System.out.println("[APROVADO] Arvore preservada");
    }

    private static NoOperador tabela(String nome) {
        return new NoOperador(TipoOperador.TABELA, nome);
    }

    private static NoOperador no(
            TipoOperador tipo,
            String valor,
            NoOperador esquerdo,
            NoOperador direito
    ) {
        NoOperador resultado = new NoOperador(tipo, valor);
        resultado.setEsquerdo(esquerdo);
        resultado.setDireito(direito);
        return resultado;
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
