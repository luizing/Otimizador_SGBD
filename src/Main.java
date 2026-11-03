import Parser.Parser;
import Parser.QuerryObj;
import Parser.SyntaxException;
import Parser.SemanticException;

import RelationalAlgebra.RelationalAlgebraConverter;
import RelationalAlgebra.RelationalExpression;
import RelationalAlgebra.RelationalFormatter;

import java.util.List;

import UI.MainWindow;

public class Main {

    public static void main(String[] args) {

        List<String> queries = List.of(

                // Teste 1 - Cliente e pedidos
                """
                SELECT cliente.nome, pedido.idPedido, pedido.DataPedido, pedido.ValorTotalPedido
                FROM Cliente
                JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
                WHERE cliente.TipoCliente_idTipoCliente = 1
                  AND pedido.ValorTotalPedido = 0;
                """,

                // Teste 2 - Cliente, pedidos e status
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

                // Teste 3 - Cliente, pedidos, status e produtos
                """
                SELECT cliente.nome, pedido.idPedido, pedido.DataPedido,
                       Status.descricao, pedido.ValorTotalPedido, produto.QuantEstoque
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

                // Teste 4 - Consulta completa com múltiplos JOINs
                """
                SELECT cliente.nome, tipocliente.descricao,
                       pedido.idPedido, pedido.DataPedido, Status.descricao,
                       pedido.ValorTotalPedido, categoria.descricao, produto.QuantEstoque
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
                """
        );

        // HU01 - Parser
        Parser parser = new Parser();

        // HU02 - Componentes de conversão
        RelationalAlgebraConverter converter =
                new RelationalAlgebraConverter();

        RelationalFormatter formatter =
                new RelationalFormatter();

        for (String sql : queries) {

            System.out.println("\n==================================");
            System.out.println("CONSULTA SQL:");
            System.out.println(sql);

            try {

                // ETAPA 1 - Parsing e validação (HU01)
                QuerryObj result = parser.readRaw(sql);

                System.out.println("\n[HU01] Consulta válida!");
                System.out.println(result);

                // ETAPA 2 - Conversão para álgebra relacional (HU02)
                RelationalExpression expression =
                        converter.convert(result);

                // ETAPA 3 - Formatação da expressão
                String algebra = formatter.format(expression);

                System.out.println("\n[HU02] ÁLGEBRA RELACIONAL:");
                System.out.println(algebra);

            } catch (SyntaxException e) {

                System.out.println("\nERRO SINTÁTICO:");
                System.out.println(e.getMessage());

            } catch (SemanticException e) {

                System.out.println("\nERRO SEMÂNTICO:");
                System.out.println(e.getMessage());

            } catch (IllegalArgumentException e) {

                System.out.println("\nERRO NA CONVERSÃO:");
                System.out.println(e.getMessage());

            }

            System.out.println("\n==================================");

        }
        
        MainWindow.main(args);
    }
}
