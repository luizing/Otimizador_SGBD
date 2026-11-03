package UI;

import Graph.OperatorGraphBuilder;
import Graph.OperatorGraphPanel;
import Graph.OperatorNode;
import Parser.Parser;
import Parser.QuerryObj;
import Parser.SemanticException;
import Parser.SyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class MainWindow extends JFrame {

    private static final String EXAMPLE_QUERY = """
            SELECT cliente.nome, pedido.idPedido, pedido.DataPedido,
                   Status.descricao, pedido.ValorTotalPedido
            FROM Cliente
            JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
            JOIN Status ON Status.idstatus = Pedido.status_idstatus
            WHERE Status.descricao = 'Aberto'
              AND cliente.TipoCliente_idTipoCliente = 1
              AND pedido.ValorTotalPedido = 0;
            """;

    private static final Color SUCCESS_COLOR = new Color(0x1E, 0x7B, 0x3A);
    private static final Color ERROR_COLOR = new Color(0xB3, 0x26, 0x1E);

    private final JTextArea sqlInput = new JTextArea(EXAMPLE_QUERY, 8, 80);
    private final JTextArea statusArea = new JTextArea(2, 80);
    private final OperatorGraphPanel originalGraphPanel = new OperatorGraphPanel();

    public MainWindow() {
        super("Processador de Consultas");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildInputPanel(), BorderLayout.NORTH);
        add(buildResultTabs(), BorderLayout.CENTER);

        setSize(1100, 800);
        setLocationRelativeTo(null);
    }

    private JPanel buildInputPanel() {
        sqlInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

        JButton analyzeButton = new JButton("Analisar");
        analyzeButton.addActionListener(event -> analyze());

        JPanel actions = new JPanel(new BorderLayout(8, 8));
        actions.add(analyzeButton, BorderLayout.WEST);

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);
        actions.add(statusArea, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(new JLabel("Consulta SQL:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(sqlInput), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JTabbedPane buildResultTabs() {
        JScrollPane graphScroll = new JScrollPane(originalGraphPanel);
        graphScroll.setPreferredSize(new Dimension(800, 500));
        graphScroll.getVerticalScrollBar().setUnitIncrement(16);
        graphScroll.getHorizontalScrollBar().setUnitIncrement(16);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Grafo original", graphScroll);

        return tabs;
    }

    private void analyze() {
        try {
            QuerryObj query = new Parser().readRaw(sqlInput.getText());
            OperatorNode graph = OperatorGraphBuilder.build(query);

            originalGraphPanel.setGraph(graph);
            showStatus("Consulta válida.", SUCCESS_COLOR);

        } catch (SyntaxException | IllegalArgumentException e) {
            // O tokenizer sinaliza caractere inválido e string não finalizada
            // com IllegalArgumentException; ambos são falhas de gramática.
            showError("Erro sintático", e.getMessage());

        } catch (SemanticException e) {
            showError("Erro semântico", e.getMessage());
        }
    }

    private void showError(String kind, String message) {
        originalGraphPanel.setGraph(null);

        // As mensagens do parser já começam com "Erro sintático na posição"
        String text = message.startsWith(kind)
                ? message
                : kind + ": " + message;

        showStatus(text, ERROR_COLOR);
    }

    private void showStatus(String message, Color color) {
        statusArea.setForeground(color);
        statusArea.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                // Mantém o tema padrão do Swing
            }

            new MainWindow().setVisible(true);
        });
    }
}
