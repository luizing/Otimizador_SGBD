
package hu04.otimizacao;

import hu04.modelo.NoOperador;

public class OtimizarConsulta {

    public NoOperador otimizar(NoOperador arvoreOriginal) {
        if (arvoreOriginal == null) {
            throw new IllegalArgumentException(
                "A arvore original nao pode ser nula."
            );
        }

        NoOperador arvoreOtimizada =
            ProdutoCartesianoHeuristica.evitarProdutoCartesiano(
                arvoreOriginal
            );

        arvoreOtimizada = DecomposicaoSelecoes.decompor(
            arvoreOtimizada
        );

        arvoreOtimizada = Heuristica.reduzirTuplas(
            arvoreOtimizada
        );

        arvoreOtimizada = Heuristica.priorizarJuncoes(
            arvoreOtimizada
        );

        arvoreOtimizada = Heuristica.reduzirAtributos(
            arvoreOtimizada
        );

        arvoreOtimizada = SimplificadorProjecoes.simplificar(
            arvoreOtimizada
        );

        return arvoreOtimizada;
    }
}
