
package hu04.otimizacao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import java.util.ArrayList;
import java.util.List;

public class DecomposicaoSelecoes {

    public static NoOperador decompor(NoOperador raiz) {
        if (raiz == null) {
            return null;
        }

        return decomporRecursivamente(raiz.copiar());
    }

    private static NoOperador decomporRecursivamente(
            NoOperador no
    ) {
        if (no == null) {
            return null;
        }

        no.setEsquerdo(
                decomporRecursivamente(no.getEsquerdo())
        );

        no.setDireito(
                decomporRecursivamente(no.getDireito())
        );

        if (no.getTipo() != TipoOperador.SELECAO
                || no.getEsquerdo() == null
                || no.getDireito() != null) {
            return no;
        }

        List<String> condicoes = separarCondicoes(no.getValor());

        if (condicoes.size() <= 1) {
            return no;
        }

        NoOperador resultado = no.getEsquerdo();

        for (int i = condicoes.size() - 1; i >= 0; i--) {
            NoOperador selecao = new NoOperador(
                    TipoOperador.SELECAO,
                    condicoes.get(i)
            );

            selecao.setEsquerdo(resultado);
            resultado = selecao;
        }

        return resultado;
    }

    private static List<String> separarCondicoes(String texto) {
        List<String> resultado = new ArrayList<>();

        if (texto == null || texto.isBlank()) {
            return resultado;
        }

        String expressao = removerParentesesExternos(texto);

        List<String> partes = dividirAndExterno(expressao);

        if (partes.size() <= 1) {
            resultado.add(expressao);
            return resultado;
        }

        for (String parte : partes) {
            resultado.addAll(separarCondicoes(parte));
        }

        return resultado;
    }

    private static String removerParentesesExternos(
            String texto
    ) {
        String resultado = texto.trim();

        while (resultado.startsWith("(")
                && resultado.endsWith(")")
                && parentesesEnvolvemTudo(resultado)) {

            resultado = resultado.substring(
                    1,
                    resultado.length() - 1
            ).trim();
        }

        return resultado;
    }

    private static boolean parentesesEnvolvemTudo(
            String texto
    ) {
        int profundidade = 0;
        boolean dentroString = false;

        for (int i = 0; i < texto.length(); i++) {
            char atual = texto.charAt(i);

            if (atual == '\'') {
                if (dentroString
                        && i + 1 < texto.length()
                        && texto.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }

                dentroString = !dentroString;
                continue;
            }

            if (dentroString) {
                continue;
            }

            if (atual == '(') {
                profundidade++;
            } else if (atual == ')') {
                profundidade--;

                if (profundidade == 0
                        && i < texto.length() - 1) {
                    return false;
                }

                if (profundidade < 0) {
                    return false;
                }
            }
        }

        return profundidade == 0 && !dentroString;
    }

    private static List<String> dividirAndExterno(
            String texto
    ) {
        List<String> resultado = new ArrayList<>();

        int inicio = 0;
        int profundidade = 0;
        boolean dentroString = false;

        for (int i = 0; i < texto.length(); i++) {
            char atual = texto.charAt(i);

            if (atual == '\'') {
                if (dentroString
                        && i + 1 < texto.length()
                        && texto.charAt(i + 1) == '\'') {
                    i++;
                    continue;
                }

                dentroString = !dentroString;
                continue;
            }

            if (dentroString) {
                continue;
            }

            if (atual == '(') {
                profundidade++;
                continue;
            }

            if (atual == ')') {
                profundidade--;
                continue;
            }

            if (profundidade == 0 && ehAnd(texto, i)) {
                String parte = texto.substring(
                        inicio, i
                ).trim();

                if (parte.isEmpty()) {
                    return List.of(texto);
                }

                resultado.add(parte);

                i += 2;
                inicio = i + 1;
            }
        }

        String ultima = texto.substring(inicio).trim();

        if (ultima.isEmpty()) {
            return List.of(texto);
        }

        resultado.add(ultima);

        return resultado;
    }

    private static boolean ehAnd(String texto, int indice) {
        if (indice + 3 > texto.length()) {
            return false;
        }

        if (!texto.regionMatches(
                true, indice, "AND", 0, 3)) {
            return false;
        }

        boolean antes = indice == 0
                || !ehIdentificador(texto.charAt(indice - 1));

        boolean depois = indice + 3 == texto.length()
                || !ehIdentificador(texto.charAt(indice + 3));

        return antes && depois;
    }

    private static boolean ehIdentificador(char caractere) {
        return Character.isLetterOrDigit(caractere)
                || caractere == '_';
    }
}
