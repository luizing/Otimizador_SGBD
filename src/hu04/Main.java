
package hu04;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;
import hu04.otimizacao.OtimizarConsulta;
import hu04.visualizacao.VisualizadorArvore;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        NoOperador cliente = new NoOperador(
            TipoOperador.TABELA, "Cliente"
        );

        NoOperador pedido = new NoOperador(
            TipoOperador.TABELA, "Pedido"
        );

        NoOperador produto = new NoOperador(
            TipoOperador.PRODUTO_CARTESIANO,
            "Cliente x Pedido"
        );

        produto.setEsquerdo(cliente);
        produto.setDireito(pedido);

        NoOperador selecao = new NoOperador(
            TipoOperador.SELECAO,
            "Cliente.idCliente = Pedido.Cliente_idCliente"
        );

        selecao.setEsquerdo(produto);

        NoOperador projecao = new NoOperador(
            TipoOperador.PROJECAO,
            "Cliente.Nome, Pedido.idPedido"
        );

        projecao.setAtributos(
            List.of("Cliente.Nome", "Pedido.idPedido")
        );

        projecao.setEsquerdo(selecao);

        OtimizarConsulta otimizador = new OtimizarConsulta();
        NoOperador arvoreOtimizada = otimizador.otimizar(projecao);

        System.out.println("=== ARVORE ORIGINAL ===");
        projecao.exibirArvore();

        System.out.println("\n=== ARVORE OTIMIZADA ===");
        arvoreOtimizada.exibirArvore();

        VisualizadorArvore.exibir(
            projecao, arvoreOtimizada
        );
    }
}
