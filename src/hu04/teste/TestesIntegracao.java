
package hu04.teste;

import Parser.Parser;
import Parser.QuerryObj;

import RelationalAlgebra.RelationalAlgebraConverter;
import RelationalAlgebra.RelationalExpression;

import hu04.integracao.AdaptadorAlgebra;
import hu04.integracao.AdaptadorGrafico;
import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;
import hu04.otimizacao.OtimizarConsulta;

import Graph.OperatorNode;

import java.util.ArrayList;
import java.util.List;

public class TestesIntegracao {

    private static int verificacoes = 0;

    public static void main(String[] args) {

        testar(
            "Consulta 1 - 2 tabelas",
            """
            SELECT cliente.nome, pedido.idPedido, pedido.DataPedido,
                   pedido.ValorTotalPedido
            FROM Cliente
            JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
            WHERE cliente.TipoCliente_idTipoCliente = 1
              AND pedido.ValorTotalPedido = 0;
            """,
            2, 1, 2
        );

        testar(
            "Consulta 2 - 3 tabelas",
            """
            SELECT cliente.nome, pedido.idPedido, pedido.DataPedido,
                   Status.descricao, pedido.ValorTotalPedido
            FROM Cliente
            JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
            JOIN Status ON Status.idstatus = Pedido.status_idstatus
            WHERE Status.descricao = 'Aberto'
              AND cliente.TipoCliente_idTipoCliente = 1
              AND pedido.ValorTotalPedido = 0;
            """,
            3, 2, 3
        );

        testar(
            "Consulta 3 - 5 tabelas",
            """
            SELECT cliente.nome, pedido.idPedido, pedido.DataPedido,
                   Status.descricao, pedido.ValorTotalPedido,
                   produto.QuantEstoque
            FROM Cliente
            JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
            JOIN Status ON Status.idstatus = Pedido.status_idstatus
            JOIN pedido_has_produto
                ON pedido.idPedido = pedido_has_produto.Pedido_idPedido
            JOIN produto
                ON produto.idProduto = pedido_has_produto.Produto_idProduto
            WHERE Status.descricao = 'Aberto'
              AND cliente.TipoCliente_idTipoCliente = 1
              AND pedido.ValorTotalPedido = 0
              AND produto.QuantEstoque > 0;
            """,
            5, 4, 4
        );

        testar(
            "Consulta 4 - 8 tabelas",
            """
            SELECT cliente.nome, tipocliente.descricao,
                   pedido.idPedido, pedido.DataPedido,
                   Status.descricao, pedido.ValorTotalPedido,
                   categoria.descricao, produto.QuantEstoque
            FROM Cliente
            JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
            JOIN tipocliente
                ON cliente.tipocliente_idtipocliente = tipocliente.idTipoCliente
            JOIN endereco
                ON cliente.idcliente = endereco.Cliente_idCliente
            JOIN Status ON Status.idstatus = Pedido.status_idstatus
            JOIN pedido_has_produto
                ON pedido.idPedido = pedido_has_produto.Pedido_idPedido
            JOIN produto
                ON produto.idProduto = pedido_has_produto.Produto_idProduto
            JOIN categoria
                ON categoria.idcategoria = produto.Categoria_idCategoria
            WHERE Status.descricao = 'Aberto'
              AND cliente.email = 'Luffy@gmail.com'
              AND pedido.ValorTotalPedido = 0
              AND produto.preco > 5000
              AND endereco.cidade = 'Gramado';
            """,
            8, 7, 5
        );

        System.out.println();
        System.out.println("==================================");
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println("Verificacoes: " + verificacoes);
        System.out.println("==================================");
    }

    private static void testar(
            String nome,
            String sql,
            int tabelasEsperadas,
            int juncoesEsperadas,
            int selecoesEsperadas
    ) {

        QuerryObj consulta = new Parser().readRaw(sql);

        RelationalExpression expressao =
                new RelationalAlgebraConverter().convert(consulta);

        NoOperador original =
                new AdaptadorAlgebra().converter(expressao);

        NoOperador otimizada =
                new OtimizarConsulta().otimizar(original);

        OperatorNode grafo =
                new AdaptadorGrafico().converter(otimizada);

        verificar(grafo != null, nome + ": grafo nulo");

        verificar(
                contar(otimizada, TipoOperador.TABELA)
                        == tabelasEsperadas,
                nome + ": quantidade incorreta de tabelas"
        );

        verificar(
                contar(otimizada, TipoOperador.JUNCAO)
                        == juncoesEsperadas,
                nome + ": quantidade incorreta de juncoes"
        );

        verificar(
                contar(otimizada, TipoOperador.SELECAO)
                        == selecoesEsperadas,
                nome + ": quantidade incorreta de selecoes"
        );

        verificar(
                contar(original, TipoOperador.TABELA)
                        == contar(otimizada, TipoOperador.TABELA),
                nome + ": tabelas nao preservadas"
        );

        verificar(
                coletarValores(original, TipoOperador.JUNCAO)
                        .containsAll(
                            coletarValores(
                                otimizada, TipoOperador.JUNCAO
                            )
                        ),
                nome + ": condicoes de juncao alteradas"
        );

        verificar(
                coletarValores(otimizada, TipoOperador.JUNCAO)
                        .containsAll(
                            coletarValores(
                                original, TipoOperador.JUNCAO
                            )
                        ),
                nome + ": condicoes de juncao perdidas"
        );

        verificar(
                original.getTipo() == TipoOperador.PROJECAO
                        && otimizada.getTipo() == TipoOperador.PROJECAO,
                nome + ": projecao final nao preservada"
        );

        verificar(
                original.getValor().equals(otimizada.getValor()),
                nome + ": atributos finais alterados"
        );

        verificar(
                selecoesNasTabelas(otimizada),
                nome + ": selecoes nao foram antecipadas"
        );

        System.out.println("[APROVADO] " + nome);
    }

    private static int contar(
            NoOperador no,
            TipoOperador tipo
    ) {
        if (no == null) {
            return 0;
        }

        int atual = no.getTipo() == tipo ? 1 : 0;

        return atual
                + contar(no.getEsquerdo(), tipo)
                + contar(no.getDireito(), tipo);
    }

    private static List<String> coletarValores(
            NoOperador no,
            TipoOperador tipo
    ) {
        List<String> valores = new ArrayList<>();
        coletarValoresRecursivo(no, tipo, valores);
        return valores;
    }

    private static void coletarValoresRecursivo(
            NoOperador no,
            TipoOperador tipo,
            List<String> valores
    ) {
        if (no == null) {
            return;
        }

        if (no.getTipo() == tipo) {
            valores.add(no.getValor());
        }

        coletarValoresRecursivo(
                no.getEsquerdo(), tipo, valores
        );

        coletarValoresRecursivo(
                no.getDireito(), tipo, valores
        );
    }

    private static boolean selecoesNasTabelas(NoOperador no) {
        if (no == null) {
            return true;
        }

        if (no.getTipo() == TipoOperador.SELECAO) {
            NoOperador filho = no.getEsquerdo();

            while (filho != null
                    && filho.getTipo() == TipoOperador.PROJECAO) {
                filho = filho.getEsquerdo();
            }

            if (filho == null
                    || filho.getTipo() != TipoOperador.TABELA) {
                return false;
            }
        }

        return selecoesNasTabelas(no.getEsquerdo())
                && selecoesNasTabelas(no.getDireito());
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
