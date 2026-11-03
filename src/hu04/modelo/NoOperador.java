package hu04.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NoOperador {

    private TipoOperador tipo;
    private String valor;
    private List<String> atributos;
    private NoOperador esquerdo;
    private NoOperador direito;

    public NoOperador(TipoOperador tipo, String valor) {
        this.tipo = Objects.requireNonNull(tipo);
        this.valor = valor;
        this.atributos = new ArrayList<>();
    }

    public TipoOperador getTipo() {
        return tipo;
    }

    public void setTipo(TipoOperador tipo) {
        this.tipo = Objects.requireNonNull(tipo);
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public List<String> getAtributos() {
        return new ArrayList<>(atributos);
    }

    public void setAtributos(List<String> atributos) {
        this.atributos = new ArrayList<>(atributos);
    }

    public NoOperador getEsquerdo() {
        return esquerdo;
    }

    public void setEsquerdo(NoOperador esquerdo) {
        this.esquerdo = esquerdo;
    }

    public NoOperador getDireito() {
        return direito;
    }

    public void setDireito(NoOperador direito) {
        this.direito = direito;
    }

    public boolean ehFolha() {
        return esquerdo == null && direito == null;
    }

    public NoOperador copiar() {
        NoOperador copia = new NoOperador(tipo, valor);
        copia.setAtributos(atributos);

        if (esquerdo != null) {
            copia.setEsquerdo(esquerdo.copiar());
        }

        if (direito != null) {
            copia.setDireito(direito.copiar());
        }

        return copia;
    }

    public void exibirArvore() {
        exibirArvore("", true);
    }

    private void exibirArvore(String prefixo, boolean ultimo) {
        System.out.println(prefixo + (ultimo ? "└── " : "├── ") + tipo + ": " + valor);

        List<NoOperador> filhos = new ArrayList<>();

        if (esquerdo != null) {
            filhos.add(esquerdo);
        }

        if (direito != null) {
            filhos.add(direito);
        }

        for (int i = 0; i < filhos.size(); i++) {
            filhos.get(i).exibirArvore(prefixo + (ultimo ? "    " : "│   "), i == filhos.size() - 1);
        }
    }
}