
package hu04.visualizacao;

import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.IdentityHashMap;
import java.util.Map;

public class VisualizadorArvore extends JFrame {

    public VisualizadorArvore(
        NoOperador arvoreOriginal,
        NoOperador arvoreOtimizada
    ) {
        super("Processador de Consultas - HU04");

        if (arvoreOriginal == null || arvoreOtimizada == null) {
            throw new IllegalArgumentException(
                "As arvores nao podem ser nulas."
            );
        }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setMinimumSize(new Dimension(900, 500));
        setLocationRelativeTo(null);

        JPanel principal = new JPanel(new GridLayout(1, 2, 12, 0));
        principal.setBorder(
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );
        principal.setBackground(new Color(238, 242, 247));

        principal.add(criarPainel(
            "Arvore Original", arvoreOriginal
        ));

        principal.add(criarPainel(
            "Arvore Otimizada", arvoreOtimizada
        ));

        setContentPane(principal);
    }

    private JPanel criarPainel(String titulo, NoOperador raiz) {
        JPanel painel = new JPanel(new BorderLayout(0, 10));
        painel.setBackground(Color.WHITE);
        painel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(210, 218, 228)
                ),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
            )
        );

        JLabel label = new JLabel(titulo, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));

        painel.add(label, BorderLayout.NORTH);

        PainelArvore desenho = new PainelArvore(raiz);
        JScrollPane scroll = new JScrollPane(desenho);

        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);

        painel.add(scroll, BorderLayout.CENTER);

        return painel;
    }

    public static void exibir(
        NoOperador original,
        NoOperador otimizada
    ) {
        SwingUtilities.invokeLater(() -> {
            VisualizadorArvore janela = new VisualizadorArvore(
                original.copiar(), otimizada.copiar()
            );

            janela.setVisible(true);
        });
    }

    private static class PainelArvore extends JPanel {

        private static final int LARGURA_NO = 240;
        private static final int ALTURA_NO = 64;
        private static final int ESPACO_HORIZONTAL = 32;
        private static final int ESPACO_VERTICAL = 110;
        private static final int MARGEM = 45;

        private final NoOperador raiz;
        private final Map<NoOperador, Point> posicoes;
        private int proximaFolha;

        public PainelArvore(NoOperador raiz) {
            this.raiz = raiz;
            this.posicoes = new IdentityHashMap<>();

            setBackground(Color.WHITE);

            calcularLayout();
        }

        private void calcularLayout() {
            proximaFolha = 0;
            posicoes.clear();

            posicionar(raiz, 0);

            int folhas = Math.max(1, contarFolhas(raiz));
            int profundidade = profundidade(raiz);

            int largura = MARGEM * 2
                + folhas * (LARGURA_NO + ESPACO_HORIZONTAL);

            int altura = MARGEM * 2
                + profundidade * ESPACO_VERTICAL;

            setPreferredSize(new Dimension(largura, altura));
        }

        private int posicionar(NoOperador no, int nivel) {
            if (no == null) {
                return 0;
            }

            NoOperador esquerdo = no.getEsquerdo();
            NoOperador direito = no.getDireito();

            int x;

            if (esquerdo == null && direito == null) {
                x = MARGEM + proximaFolha
                    * (LARGURA_NO + ESPACO_HORIZONTAL);

                proximaFolha++;
            } else {
                Integer xEsquerdo = null;
                Integer xDireito = null;

                if (esquerdo != null) {
                    xEsquerdo = posicionar(esquerdo, nivel + 1);
                }

                if (direito != null) {
                    xDireito = posicionar(direito, nivel + 1);
                }

                if (xEsquerdo != null && xDireito != null) {
                    x = (xEsquerdo + xDireito) / 2;
                } else {
                    x = xEsquerdo != null ? xEsquerdo : xDireito;
                }
            }

            int y = MARGEM + nivel * ESPACO_VERTICAL;

            posicoes.put(no, new Point(x, y));

            return x;
        }

        private int contarFolhas(NoOperador no) {
            if (no == null) {
                return 0;
            }

            if (no.ehFolha()) {
                return 1;
            }

            return contarFolhas(no.getEsquerdo())
                + contarFolhas(no.getDireito());
        }

        private int profundidade(NoOperador no) {
            if (no == null) {
                return 0;
            }

            return 1 + Math.max(
                profundidade(no.getEsquerdo()),
                profundidade(no.getDireito())
            );
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            desenharConexoes(g2, raiz);
            desenharNos(g2, raiz);

            g2.dispose();
        }

        private void desenharConexoes(
            Graphics2D g2, NoOperador no
        ) {
            if (no == null) {
                return;
            }

            Point origem = posicoes.get(no);

            for (NoOperador filho : new NoOperador[]{
                no.getEsquerdo(), no.getDireito()
            }) {
                if (filho == null) {
                    continue;
                }

                Point destino = posicoes.get(filho);

                g2.setColor(new Color(125, 137, 153));
                g2.setStroke(new BasicStroke(2));

                g2.draw(new Line2D.Double(
                    origem.x + LARGURA_NO / 2.0,
                    origem.y + ALTURA_NO,
                    destino.x + LARGURA_NO / 2.0,
                    destino.y
                ));

                desenharConexoes(g2, filho);
            }
        }

        private void desenharNos(
            Graphics2D g2, NoOperador no
        ) {
            if (no == null) {
                return;
            }

            Point posicao = posicoes.get(no);
            Color cor = corOperador(no.getTipo());

            g2.setColor(cor);
            g2.fillRoundRect(
                posicao.x, posicao.y,
                LARGURA_NO, ALTURA_NO, 14, 14
            );

            g2.setColor(Color.WHITE);

            g2.setFont(new Font(
                "SansSerif", Font.BOLD, 12
            ));

            FontMetrics fm = g2.getFontMetrics();

            String titulo = nomeOperador(no.getTipo());
            int larguraTitulo = fm.stringWidth(titulo);

            g2.drawString(
                titulo,
                posicao.x + (LARGURA_NO - larguraTitulo) / 2,
                posicao.y + 23
            );

            g2.setFont(new Font(
                "SansSerif", Font.PLAIN, 11
            ));

            String valor = no.getValor() == null
                ? "" : no.getValor();

            desenharValor(g2, valor, posicao);

            desenharNos(g2, no.getEsquerdo());
            desenharNos(g2, no.getDireito());
        }

        private void desenharValor(
            Graphics2D g2, String valor, Point posicao
        ) {
            FontMetrics fm = g2.getFontMetrics();
            int larguraMaxima = LARGURA_NO - 20;

            String texto = valor;

            while (!texto.isEmpty()
                    && fm.stringWidth(texto) > larguraMaxima) {
                texto = texto.substring(0, texto.length() - 1);
            }

            if (!texto.equals(valor) && texto.length() > 3) {
                texto = texto.substring(0, texto.length() - 3)
                    + "...";
            }

            int larguraTexto = fm.stringWidth(texto);

            g2.drawString(
                texto,
                posicao.x + (LARGURA_NO - larguraTexto) / 2,
                posicao.y + 45
            );
        }

        private String nomeOperador(TipoOperador tipo) {
            switch (tipo) {
                case TABELA:
                    return "TABELA";
                case SELECAO:
                    return "SELECAO (σ)";
                case PROJECAO:
                    return "PROJECAO (π)";
                case JUNCAO:
                    return "JUNCAO (⋈)";
                case PRODUTO_CARTESIANO:
                    return "PRODUTO CARTESIANO (×)";
                default:
                    return tipo.name();
            }
        }

        private Color corOperador(TipoOperador tipo) {
            switch (tipo) {
                case TABELA:
                    return new Color(71, 85, 105);
                case SELECAO:
                    return new Color(37, 99, 235);
                case PROJECAO:
                    return new Color(124, 58, 237);
                case JUNCAO:
                    return new Color(13, 148, 136);
                case PRODUTO_CARTESIANO:
                    return new Color(220, 38, 38);
                default:
                    return Color.DARK_GRAY;
            }
        }
    }
}
