
package hu05.modelo;

import hu04.modelo.TipoOperador;
import java.util.List;
import java.util.Objects;

public class PassoExecucao {

    private final int numero;
    private final TipoOperador tipo;
    private final String descricao;
    private final List<Integer> dependencias;
    private final List<String> heuristicas;

    public PassoExecucao(
            int numero,
            TipoOperador tipo,
            String descricao,
            List<Integer> dependencias,
            List<String> heuristicas
    ) {
        if (numero < 1) {
            throw new IllegalArgumentException(
                "O numero do passo deve ser positivo."
            );
        }

        this.numero = numero;
        this.tipo = Objects.requireNonNull(tipo);
        this.descricao = Objects.requireNonNull(descricao);
        this.dependencias = List.copyOf(dependencias);
        this.heuristicas = List.copyOf(heuristicas);
    }

    public int getNumero() {
        return numero;
    }

    public TipoOperador getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<Integer> getDependencias() {
        return dependencias;
    }

    public List<String> getHeuristicas() {
        return heuristicas;
    }
}
