package Graph;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Desenha o grafo de operadores: folhas (tabelas) embaixo, projeção final no
 * topo e setas indicando o fluxo dos resultados intermediários.
 */
public class OperatorGraphPanel extends JPanel {

    private static final int MARGIN = 24;
    private static final int H_GAP = 28;
    private static final int V_GAP = 42;
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 8;
    private static final int MAX_TEXT_WIDTH = 300;
    private static final int ARROW_SIZE = 9;

    private static final Color EDGE_COLOR = new Color(0x55, 0x5F, 0x6B);
    private static final Color TEXT_COLOR = new Color(0x1F, 0x24, 0x2B);

    private final Font nodeFont = findSymbolFont();

    private Placed root;

    public OperatorGraphPanel() {
        setBackground(Color.WHITE);
    }

    public void setGraph(OperatorNode graph) {
        root = null;
        Dimension size = new Dimension(0, 0);

        if (graph != null) {
            root = measure(graph);

            List<Integer> levelHeights = new ArrayList<>();
            collectLevelHeights(root, 0, levelHeights);

            place(root, MARGIN, 0, levelHeights);

            int height = levelHeights.stream()
                    .mapToInt(h -> h + V_GAP)
                    .sum() - V_GAP;

            size = new Dimension(
                    root.subtreeWidth + 2 * MARGIN,
                    height + 2 * MARGIN
            );
        }

        setPreferredSize(size);
        revalidate();
        repaint();
    }

    // Etapa 1: calcula o tamanho de cada nó e da subárvore abaixo dele
    private Placed measure(OperatorNode node) {
        FontMetrics metrics = getFontMetrics(nodeFont);

        Placed placed = new Placed(node);
        placed.lines = wrap(node.getLabel(), metrics);

        int textWidth = placed.lines.stream()
                .mapToInt(metrics::stringWidth)
                .max()
                .orElse(0);

        placed.width = textWidth + 2 * PADDING_X;
        placed.height = placed.lines.size() * metrics.getHeight()
                + 2 * PADDING_Y;

        int childrenWidth = 0;

        for (OperatorNode child : node.getChildren()) {
            Placed placedChild = measure(child);
            placed.children.add(placedChild);
            childrenWidth += placedChild.subtreeWidth;
        }

        childrenWidth += H_GAP * Math.max(0, placed.children.size() - 1);

        placed.childrenWidth = childrenWidth;
        placed.subtreeWidth = Math.max(placed.width, childrenWidth);

        return placed;
    }

    private void collectLevelHeights(
            Placed placed,
            int level,
            List<Integer> heights
    ) {
        if (heights.size() == level) {
            heights.add(0);
        }

        heights.set(level, Math.max(heights.get(level), placed.height));

        for (Placed child : placed.children) {
            collectLevelHeights(child, level + 1, heights);
        }
    }

    // Etapa 2: define a posição; cada nó fica centrado sobre sua subárvore,
    // o que garante que subárvores vizinhas nunca se sobreponham.
    private void place(
            Placed placed,
            int left,
            int level,
            List<Integer> levelHeights
    ) {
        int top = MARGIN;

        for (int i = 0; i < level; i++) {
            top += levelHeights.get(i) + V_GAP;
        }

        placed.centerX = left + placed.subtreeWidth / 2;
        placed.y = top + (levelHeights.get(level) - placed.height) / 2;

        int childLeft = left + (placed.subtreeWidth - placed.childrenWidth) / 2;

        for (Placed child : placed.children) {
            place(child, childLeft, level + 1, levelHeights);
            childLeft += child.subtreeWidth + H_GAP;
        }
    }

    private static List<String> wrap(String text, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;

            if (!line.isEmpty()
                    && metrics.stringWidth(candidate) > MAX_TEXT_WIDTH) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        lines.add(line.toString());

        return lines;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (root == null) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        g.setFont(nodeFont);

        paintEdges(g, root);
        paintNodes(g, root);

        g.dispose();
    }

    // A seta sai do filho e aponta para o pai, seguindo o fluxo dos dados
    private void paintEdges(Graphics2D g, Placed parent) {
        for (Placed child : parent.children) {

            int fromX = child.centerX;
            int fromY = child.y;
            int toX = parent.centerX;
            int toY = parent.y + parent.height;

            g.setColor(EDGE_COLOR);
            g.setStroke(new BasicStroke(1.5f));

            double angle = Math.atan2(toY - fromY, toX - fromX);

            // Termina a linha na base da seta para a ponta ficar limpa
            g.drawLine(
                    fromX,
                    fromY,
                    (int) Math.round(toX - ARROW_SIZE * Math.cos(angle)),
                    (int) Math.round(toY - ARROW_SIZE * Math.sin(angle))
            );

            paintArrowHead(g, toX, toY, angle);
            paintEdges(g, child);
        }
    }

    private void paintArrowHead(Graphics2D g, int x, int y, double angle) {
        double spread = Math.toRadians(25);
        double length = ARROW_SIZE / Math.cos(spread);

        Polygon head = new Polygon();
        head.addPoint(x, y);
        head.addPoint(
                (int) Math.round(x - length * Math.cos(angle - spread)),
                (int) Math.round(y - length * Math.sin(angle - spread))
        );
        head.addPoint(
                (int) Math.round(x - length * Math.cos(angle + spread)),
                (int) Math.round(y - length * Math.sin(angle + spread))
        );

        g.fillPolygon(head);
    }

    private void paintNodes(Graphics2D g, Placed placed) {
        int x = placed.centerX - placed.width / 2;
        int arc = placed.node.isLeaf() ? placed.height : 12;

        g.setColor(fillColor(placed.node.getType()));
        g.fillRoundRect(x, placed.y, placed.width, placed.height, arc, arc);

        g.setColor(borderColor(placed.node.getType()));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, placed.y, placed.width, placed.height, arc, arc);

        FontMetrics metrics = g.getFontMetrics();
        int baseline = placed.y + PADDING_Y + metrics.getAscent();

        g.setColor(TEXT_COLOR);

        for (String line : placed.lines) {
            g.drawString(
                    line,
                    placed.centerX - metrics.stringWidth(line) / 2,
                    baseline
            );
            baseline += metrics.getHeight();
        }

        for (Placed child : placed.children) {
            paintNodes(g, child);
        }
    }

    private static Color fillColor(OperatorType type) {
        return switch (type) {
            case PROJECTION -> new Color(0xE3, 0xEE, 0xFB);
            case SELECTION -> new Color(0xFD, 0xF0, 0xD5);
            case JOIN -> new Color(0xE2, 0xF3, 0xE6);
            case CROSS -> new Color(0xFB, 0xE4, 0xE1);
            case TABLE -> new Color(0xEE, 0xEF, 0xF1);
        };
    }

    private static Color borderColor(OperatorType type) {
        return switch (type) {
            case PROJECTION -> new Color(0x2F, 0x6D, 0xB5);
            case SELECTION -> new Color(0xB9, 0x83, 0x1A);
            case JOIN -> new Color(0x2E, 0x8B, 0x4A);
            case CROSS -> new Color(0xB3, 0x26, 0x1E);
            case TABLE -> new Color(0x6B, 0x72, 0x7C);
        };
    }

    // O símbolo ⋈ não existe em todas as fontes
    public static Font findSymbolFont() {
        String symbols = "πσ⋈×";
        int style = Font.PLAIN;
        int size = 13;

        Font defaultFont = new Font(Font.DIALOG, style, size);

        if (defaultFont.canDisplayUpTo(symbols) == -1) {
            return defaultFont;
        }

        for (String name : List.of(
                "Segoe UI Symbol", "Cambria Math", "DejaVu Sans",
                "Arial Unicode MS", "Noto Sans Math")) {

            Font font = new Font(name, style, size);

            if (font.getFamily().equals(name)
                    && font.canDisplayUpTo(symbols) == -1) {
                return font;
            }
        }

        for (Font font : GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAllFonts()) {

            if (font.canDisplayUpTo(symbols) == -1) {
                return font.deriveFont(style, (float) size);
            }
        }

        return new Font(Font.SANS_SERIF, style, size);
    }

    private static final class Placed {

        final OperatorNode node;
        final List<Placed> children = new ArrayList<>();

        List<String> lines;
        int width;
        int height;
        int childrenWidth;
        int subtreeWidth;
        int centerX;
        int y;

        Placed(OperatorNode node) {
            this.node = node;
        }
    }
}
