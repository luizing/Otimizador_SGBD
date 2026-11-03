
package hu05.servico;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.modelo.PassoExecucao;
import hu05.modelo.PlanoExecucao;

import java.util.ArrayList;
import java.util.List;

public class GeradorPlanoExecucao {

    public PlanoExecucao gerar(NoOperador arvoreOtimizada) {
        if (arvoreOtimizada == null) {
            throw new IllegalArgumentException(
                "A arvore otimizada nao pode ser nula."
            );
        }

        List<PassoExecucao> passos = new ArrayList<>();

        percorrer(arvoreOtimizada, passos);

        return new PlanoExecucao(passos);
    }

    private int percorrer(
            NoOperador no,
            List<PassoExecucao> passos
    ) {
        if (no == null) {
            return 0;
        }

        List<Integer> dependencias = new ArrayList<>();

        if (no.getEsquerdo() != null) {
            dependencias.add(
                percorrer(no.getEsquerdo(), passos)
            );
        }

        if (no.getDireito() != null) {
            dependencias.add(
                percorrer(no.getDireito(), passos)
            );
        }

        int numero = passos.size() + 1;

        PassoExecucao passo = new PassoExecucao(
            numero,
            no.getTipo(),
            criarDescricao(no),
            dependencias,
            List.of()
        );

        passos.add(passo);

        return numero;
    }

    private String criarDescricao(NoOperador no) {
        String valor = no.getValor() == null
            ? ""
            : no.getValor();

        TipoOperador tipo = no.getTipo();

        switch (tipo) {
            case TABELA:
                return "Ler tabela: " + valor;

            case SELECAO:
                return "Aplicar selecao: " + valor;

            case PROJECAO:
                return "Projetar atributos: " + valor;

            case JUNCAO:
                return "Executar juncao: " + valor;

            case PRODUTO_CARTESIANO:
                return "Executar produto cartesiano: "
                    + valor;

            default:
                throw new IllegalStateException(
                    "Operador nao suportado: " + tipo
                );
        }
    }
}
