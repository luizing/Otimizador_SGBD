package Graph;

import java.util.List;

/**
 * Nó do grafo de operadores. Cada filho produz um resultado intermediário
 * que flui para este nó, ou seja, cada aresta vai do filho para o pai.
 */
public class OperatorNode {

    private final OperatorType type;
    private final String detail;
    private final List<OperatorNode> children;

    public OperatorNode(
            OperatorType type,
            String detail,
            List<OperatorNode> children
    ) {
        this.type = type;
        this.detail = detail;
        this.children = List.copyOf(children);
    }

    public OperatorType getType() {
        return type;
    }

    // Colunas (π), condição (σ e ⋈) ou nome da tabela (folha)
    public String getDetail() {
        return detail;
    }

    public List<OperatorNode> getChildren() {
        return children;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public String getLabel() {
        return type == OperatorType.TABLE
                ? detail
                : type.symbol() + " " + detail;
    }

    // Representação textual da árvore, com a raiz na primeira linha
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        appendTo(text, 0);
        return text.toString();
    }

    private void appendTo(StringBuilder text, int depth) {
        text.append("  ".repeat(depth)).append(getLabel()).append('\n');

        for (OperatorNode child : children) {
            child.appendTo(text, depth + 1);
        }
    }
}
