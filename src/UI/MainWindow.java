
package UI;

import Graph.OperatorGraphBuilder;
import Graph.OperatorGraphPanel;
import Graph.OperatorNode;

import RelationalAlgebra.RelationalAlgebraConverter;
import RelationalAlgebra.RelationalExpression;

import hu04.integracao.AdaptadorAlgebra;
import hu04.integracao.AdaptadorGrafico;
import hu04.modelo.NoOperador;

import hu05.modelo.PassoExecucao;
import hu05.modelo.PlanoExecucao;
import hu05.rastreamento.RastreadorHeuristicas;
import hu05.servico.GeradorPlanoExecucao;

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

import java.util.List;

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

    private static final Color SUCCESS_COLOR =
            new Color(0x1E, 0x7B, 0x3A);

    private static final Color ERROR_COLOR =
            new Color(0xB3, 0x26, 0x1E);

    private final JTextArea sqlInput =
            new JTextArea(EXAMPLE_QUERY, 8, 80);

    private final JTextArea statusArea =
            new JTextArea(2, 80);

    private final OperatorGraphPanel originalGraphPanel =
            new OperatorGraphPanel();

    private final OperatorGraphPanel optimizedGraphPanel =
            new OperatorGraphPanel();

    // HU05: area de exibicao do plano de execucao.
    private final JTextArea executionPlanArea =
            new JTextArea();

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
        sqlInput.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 14)
        );

        JButton analyzeButton = new JButton("Analisar");
        analyzeButton.addActionListener(event -> analyze());

        JPanel actions = new JPanel(
                new BorderLayout(8, 8)
        );

        actions.add(analyzeButton, BorderLayout.WEST);

        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);

        actions.add(statusArea, BorderLayout.CENTER);

        JPanel panel = new JPanel(
                new BorderLayout(8, 8)
        );

        panel.setBorder(
                BorderFactory.createEmptyBorder(8, 8, 0, 8)
        );

        panel.add(
                new JLabel("Consulta SQL:"),
                BorderLayout.NORTH
        );

        panel.add(
                new JScrollPane(sqlInput),
                BorderLayout.CENTER
        );

        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JTabbedPane buildResultTabs() {
        JScrollPane graphScroll =
                new JScrollPane(originalGraphPanel);

        graphScroll.setPreferredSize(
                new Dimension(800, 500)
        );

        graphScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        graphScroll.getHorizontalScrollBar()
                .setUnitIncrement(16);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Grafo original", graphScroll);

        JScrollPane optimizedScroll =
                new JScrollPane(optimizedGraphPanel);

        optimizedScroll.setPreferredSize(
                new Dimension(800, 500)
        );

        optimizedScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        optimizedScroll.getHorizontalScrollBar()
                .setUnitIncrement(16);

        tabs.addTab("Grafo otimizado", optimizedScroll);

        // HU05: configura a terceira aba.
        executionPlanArea.setEditable(false);

        executionPlanArea.setFont(
                new Font(Font.MONOSPACED, Font.PLAIN, 13)
        );

        executionPlanArea.setLineWrap(true);
        executionPlanArea.setWrapStyleWord(true);

        executionPlanArea.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        executionPlanArea.setText(
                "Clique em Analisar para gerar o plano de execucao."
        );

        JScrollPane executionScroll =
                new JScrollPane(executionPlanArea);

        executionScroll.setPreferredSize(
                new Dimension(800, 500)
        );

        executionScroll.getVerticalScrollBar()
                .setUnitIncrement(16);

        tabs.addTab("Plano de execução", executionScroll);

        return tabs;
    }

    private void analyze() {
        try {
            QuerryObj query =
                    new Parser().readRaw(sqlInput.getText());

            // HU02: gera a arvore de algebra relacional.
            RelationalExpression expressao =
                    new RelationalAlgebraConverter()
                            .convert(query);

            // HU04: converte para o modelo do otimizador.
            NoOperador arvoreOriginal =
                    new AdaptadorAlgebra()
                            .converter(expressao);

            // HU05: rastreia as mesmas etapas de otimizacao
            // utilizadas pela HU04.
            RastreadorHeuristicas.Resultado rastreamento =
                    new RastreadorHeuristicas()
                            .rastrear(arvoreOriginal);

            NoOperador arvoreOtimizada =
                    rastreamento.getArvoreOtimizada();

            // HU05: gera o plano ordenado com heuristicas.
            PlanoExecucao plano =
                    new GeradorPlanoExecucao()
                            .gerarComHeuristicas(rastreamento);

            // Integracao HU04 -> HU03:
            // converte para o modelo grafico.
            OperatorNode grafoOtimizado =
                    new AdaptadorGrafico()
                            .converter(arvoreOtimizada);

            // HU03: desenha o resultado otimizado.
            optimizedGraphPanel.setGraph(grafoOtimizado);

            OperatorNode graph =
                    OperatorGraphBuilder.build(query);

            originalGraphPanel.setGraph(graph);

            // HU05: atualiza a terceira aba.
            executionPlanArea.setText(
                    formatExecutionPlan(plano, rastreamento)
            );

            executionPlanArea.setCaretPosition(0);

            showStatus("Consulta válida.", SUCCESS_COLOR);

        } catch (SyntaxException | IllegalArgumentException e) {
            showError("Erro sintático", e.getMessage());

        } catch (SemanticException e) {
            showError("Erro semântico", e.getMessage());
        }
    }

    /**
     * HU05: formata os passos do plano de execucao
     * e o historico das heuristicas aplicadas.
     */
    private String formatExecutionPlan(
            PlanoExecucao plano,
            RastreadorHeuristicas.Resultado rastreamento
    ) {
        StringBuilder texto = new StringBuilder();

        texto.append("PLANO DE EXECUCAO\n");
        texto.append("==============================\n\n");

        texto.append("Total de passos: ")
                .append(plano.getQuantidadePassos())
                .append("\n\n");

        for (PassoExecucao passo : plano.getPassos()) {
            texto.append("PASSO ")
                    .append(passo.getNumero())
                    .append("\n");

            texto.append("Operacao: ")
                    .append(passo.getDescricao())
                    .append("\n");

            texto.append("Dependencias: ");

            if (passo.getDependencias().isEmpty()) {
                texto.append("Nenhuma");
            } else {
                texto.append(
                        formatDependencies(
                                passo.getDependencias()
                        )
                );
            }

            texto.append("\n");

            texto.append("Heuristicas associadas:\n");

            if (passo.getHeuristicas().isEmpty()) {
                texto.append("  Nenhuma identificada\n");
            } else {
                for (String heuristica :
                        passo.getHeuristicas()) {
                    texto.append("  - ")
                            .append(heuristica)
                            .append("\n");
                }
            }

            texto.append("------------------------------\n\n");
        }

        texto.append("\nHISTORICO DAS HEURISTICAS\n");
        texto.append("==============================\n\n");

        for (RastreadorHeuristicas.Etapa etapa :
                rastreamento.getEtapas()) {

            texto.append(etapa.numero())
                    .append(". ")
                    .append(etapa.heuristica())
                    .append(" - ");

            if (etapa.alterouArvore()) {
                texto.append("APLICADA");
            } else {
                texto.append("SEM ALTERACAO");
            }

            texto.append("\n");
        }

        return texto.toString();
    }

    /**
     * HU05: apresenta os numeros dos passos
     * dos quais a operacao atual depende.
     */
    private String formatDependencies(
            List<Integer> dependencias
    ) {
        StringBuilder texto = new StringBuilder();

        for (int i = 0; i < dependencias.size(); i++) {
            if (i > 0) {
                texto.append(", ");
            }

            texto.append("Passo ")
                    .append(dependencias.get(i));
        }

        return texto.toString();
    }

    private void showError(
            String kind,
            String message
    ) {
        originalGraphPanel.setGraph(null);
        optimizedGraphPanel.setGraph(null);

        // HU05: evita mostrar um plano antigo
        // quando a consulta atual for invalida.
        executionPlanArea.setText(
                "Nao foi possivel gerar o plano de execucao."
        );

        // As mensagens do parser ja comecam com
        // "Erro sintatico na posicao".
        String text = message.startsWith(kind)
                ? message
                : kind + ": " + message;

        showStatus(text, ERROR_COLOR);
    }

    private void showStatus(
            String message,
            Color color
    ) {
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
                // Mantem o tema padrao do Swing.
            }

            new MainWindow().setVisible(true);
        });
    }
}
