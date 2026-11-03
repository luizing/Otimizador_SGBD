
package hu05.modelo;

import java.util.List;
import java.util.Objects;

public class PlanoExecucao {

    private final List<PassoExecucao> passos;

    public PlanoExecucao(List<PassoExecucao> passos) {
        this.passos = List.copyOf(
            Objects.requireNonNull(passos)
        );
    }

    public List<PassoExecucao> getPassos() {
        return passos;
    }

    public int getQuantidadePassos() {
        return passos.size();
    }

    public void exibir() {
        System.out.println("=== PLANO DE EXECUCAO ===");

        for (PassoExecucao passo : passos) {
            System.out.println(
                "Passo " + passo.getNumero()
                + " - " + passo.getDescricao()
            );

            if (!passo.getDependencias().isEmpty()) {
                System.out.println(
                    "  Depende dos passos: "
                    + passo.getDependencias()
                );
            }

            if (!passo.getHeuristicas().isEmpty()) {
                System.out.println(
                    "  Heuristicas: "
                    + String.join(
                        ", ", passo.getHeuristicas()
                    )
                );
            }
        }
    }
}
