package hu04.teste;

import Parser.Parser;
import Parser.QuerryObj;
import Parser.SemanticException;
import Parser.SyntaxException;

import RelationalAlgebra.RelationalAlgebraConverter;
import RelationalAlgebra.RelationalFormatter;

import hu04.integracao.AdaptadorAlgebra;
import hu04.integracao.AdaptadorGrafico;
import hu04.integracao.FormatadorArvore;
import hu04.modelo.NoOperador;
import hu04.modelo.TipoOperador;

import hu05.modelo.PassoExecucao;
import hu05.rastreamento.RastreadorHeuristicas;
import hu05.servico.GeradorPlanoExecucao;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Cobre, de ponta a ponta, as regras do PDF que dependem da consulta digitada.
 */
public class TestesRegressao {

    private static final String BASE = """
            SELECT cliente.nome, pedido.idpedido
            FROM cliente
            JOIN pedido ON cliente.idcliente = pedido.cliente_idcliente
            """;

    private static final String TRES_TABELAS = """
            SELECT cliente.nome
            FROM cliente
            JOIN pedido ON cliente.idcliente = pedido.cliente_idcliente
            JOIN status ON status.idstatus = pedido.status_idstatus
            """;

    private static final String OITO_TABELAS = """
            SELECT cliente.nome, tipocliente.descricao, pedido.idPedido,
                   Status.descricao, categoria.descricao, produto.QuantEstoque
            FROM Cliente
            JOIN pedido ON cliente.idcliente = pedido.Cliente_idCliente
            JOIN tipocliente
                ON cliente.tipocliente_idtipocliente = tipocliente.idTipoCliente
            JOIN endereco ON cliente.idcliente = endereco.Cliente_idCliente
            JOIN Status ON Status.idstatus = Pedido.status_idstatus
            JOIN pedido_has_produto
                ON pedido.idPedido = pedido_has_produto.Pedido_idPedido
            JOIN produto
                ON produto.idProduto = pedido_has_produto.Produto_idProduto
            JOIN categoria
                ON categoria.idcategoria = produto.Categoria_idCategoria
            WHERE Status.descricao = 'Aberto'
              AND cliente.email = 'Luffy@gmail.com'
              AND pedido.ValorTotalPedido = 0
              AND produto.preco > 5000
              AND endereco.cidade = 'Gramado';
            """;

    private static int verificacoes = 0;

    public static void main(String[] args) {
        testarAlgebraOtimizada();
        testarSelecaoAntesDaProjecao();
        testarReordenacaoComDuasSelecoes();
        testarSelecoesAntecipadas();
        testarReducaoDeAtributos();
        testarOrdemPorRestritividade();
        testarNormalizacao();
        testarCondicaoOn();
        testarErrosSemanticos();
        testarMensagensSintaticas();
        testarHeuristicasDoPlano();

        System.out.println();
        System.out.println("==================================");
        System.out.println("TODOS OS TESTES PASSARAM");
        System.out.println("Verificacoes: " + verificacoes);
        System.out.println("==================================");
    }

    private static void testarAlgebraOtimizada() {
        NoOperador arvore = otimizar(BASE + "WHERE cliente.nome = 'x'");
        String texto = new FormatadorArvore().formatar(arvore);

        verificar(texto.startsWith("π_{"), "Algebra sem projecao: " + texto);
        verificar(texto.contains("σ_{"), "Algebra sem selecao: " + texto);
        verificar(texto.contains("⋈_{"), "Algebra sem juncao: " + texto);

        System.out.println("[APROVADO] Algebra otimizada");
    }

    private static void testarSelecaoAntesDaProjecao() {
        for (String sql : List.of(
                OITO_TABELAS,
                BASE + "WHERE cliente.nome = 'x' AND cliente.email = 'y'",
                "SELECT cliente.nome FROM cliente WHERE cliente.email = 'x'"
        )) {
            verificar(
                    !projecaoAbaixoDeSelecao(otimizar(sql)),
                    "Projecao executada antes da selecao: " + sql
            );
        }

        System.out.println("[APROVADO] Selecao antes da projecao");
    }

    private static void testarReordenacaoComDuasSelecoes() {
        List<String> uma = juncoesNaOrdem(otimizar(
                TRES_TABELAS + "WHERE status.descricao = 'A'"
        ));

        List<String> duas = juncoesNaOrdem(otimizar(
                TRES_TABELAS + "WHERE status.descricao = 'A' "
                        + "AND status.idstatus = 1"
        ));

        verificar(
                uma.get(0).contains("Status.idStatus"),
                "Tabela restritiva deveria abrir a ordem: " + uma
        );

        verificar(
                duas.equals(uma),
                "Duas selecoes mudaram a ordem: " + duas
        );

        System.out.println("[APROVADO] Reordenacao com duas selecoes");
    }

    private static void testarSelecoesAntecipadas() {
        // Cada condicao cita uma so tabela e precisa chegar a ela.
        for (String condicao : List.of(
                "email = 'x'",
                "1 = cliente.idcliente",
                "cliente.idcliente > cliente.tipocliente_idtipocliente"
        )) {
            verificar(
                    selecaoAcimaDeJuncao(otimizar(
                            BASE + "WHERE " + condicao
                    )) == 0,
                    "Selecao nao antecipada: " + condicao
            );
        }

        NoOperador entreTabelas = otimizar(
                BASE + "WHERE cliente.idcliente = pedido.idpedido "
                        + "AND cliente.nome = 'x'"
        );

        verificar(
                selecaoAcimaDeJuncao(entreTabelas) == 1,
                "So a condicao entre tabelas fica acima da juncao"
        );

        System.out.println("[APROVADO] Selecoes antecipadas");
    }

    private static void testarReducaoDeAtributos() {
        for (String sql : List.of(
                BASE + "WHERE email = 'x'",
                BASE + "WHERE pedido.idpedido > pedido.status_idstatus",
                BASE + "WHERE cliente.idcliente = pedido.idpedido",
                """
                SELECT email, idpedido FROM cliente
                JOIN pedido ON cliente.idcliente = pedido.cliente_idcliente
                """,
                """
                SELECT cliente.nome FROM cliente
                JOIN pedido ON cliente.idcliente > pedido.cliente_idcliente
                """
        )) {
            verificar(
                    contar(otimizar(sql), TipoOperador.PROJECAO) > 1,
                    "Atributos nao reduzidos: " + sql
            );
        }

        // A coluna de uma condicao superior precisa continuar disponivel.
        List<String> valores = new ArrayList<>();
        coletar(
                otimizar(BASE + "WHERE cliente.email = pedido.status_idstatus"),
                TipoOperador.PROJECAO,
                valores
        );

        verificar(
                valores.stream().anyMatch(v -> v.contains("Cliente.Email"))
                        && valores.stream().anyMatch(
                                v -> v.contains("Pedido.Status_idStatus")),
                "Reducao removeu coluna usada: " + valores
        );

        System.out.println("[APROVADO] Reducao de atributos");
    }

    private static void testarOrdemPorRestritividade() {
        NoOperador arvore = otimizar("""
                SELECT produto.nome FROM produto
                WHERE produto.nome <> 'a'
                  AND produto.preco > 1
                  AND produto.idproduto = 2
                """);

        List<String> selecoes = new ArrayList<>();
        coletar(arvore, TipoOperador.SELECAO, selecoes);

        // O percurso desce da raiz: a ultima selecao executa primeiro.
        verificar(
                selecoes.get(0).contains("<>")
                        && selecoes.get(1).contains(">")
                        && selecoes.get(2).contains("="),
                "Ordem de restritividade incorreta: " + selecoes
        );

        System.out.println("[APROVADO] Ordem por restritividade");
    }

    private static void testarNormalizacao() {
        verificar(
                algebra("sElEcT   NOME \n\n FrOm    cLiEnTe   ;")
                        .equals("π_{Cliente.Nome}(Cliente)"),
                "Caixa e espacos nao foram normalizados"
        );

        List<String> valores = new ArrayList<>();
        coletar(
                otimizar("""
                        SELECT CLIENTE.IDCLIENTE, pedido.idpedido
                        FROM cliente
                        JOIN pedido ON cliente.idcliente = pedido.cliente_idcliente
                        WHERE Cliente.IdCliente > 1
                        """),
                TipoOperador.PROJECAO,
                valores
        );

        verificar(
                valores.stream().noneMatch(
                        v -> v.split("Cliente.idCliente", -1).length > 2),
                "Coluna repetida na projecao: " + valores
        );

        NoOperador repetida = otimizar(
                "SELECT cliente.nome, cliente.nome FROM cliente"
        );

        verificar(
                repetida.getValor().equals("Cliente.Nome, Cliente.Nome"),
                "Coluna repetida no SELECT foi perdida: " + repetida.getValor()
        );

        System.out.println("[APROVADO] Normalizacao");
    }

    private static void testarCondicaoOn() {
        verificar(
                algebra("""
                        SELECT cliente.nome FROM cliente
                        JOIN pedido ON idcliente = pedido.cliente_idcliente
                        """).contains("Cliente.idCliente = Pedido.Cliente_idCliente"),
                "ON sem tabela nao foi qualificado"
        );

        verificar(
                !algebra("""
                        SELECT cliente.nome FROM cliente
                        JOIN pedido ON (cliente.idcliente = pedido.cliente_idcliente)
                        """).isEmpty(),
                "ON com parenteses foi rejeitado"
        );

        String composto = TRES_TABELAS.replace(
                "pedido.status_idstatus",
                "pedido.status_idstatus AND status.idstatus > 0"
        );

        verificar(
                algebra(composto).contains(
                        "(Status.idStatus = Pedido.Status_idStatus "
                                + "AND Status.idStatus > 0)"
                ),
                "ON com AND nao preservou a condicao"
        );

        verificar(
                contar(otimizar(composto), TipoOperador.PROJECAO) > 1,
                "ON com AND impediu a reducao de atributos"
        );

        System.out.println("[APROVADO] Condicao ON");
    }

    private static void testarErrosSemanticos() {
        erroSemantico(
                "SELECT cliente.nome FROM cliente "
                        + "JOIN pedido ON cliente.nome = cliente.email",
                "precisa relacionar"
        );

        erroSemantico(
                "SELECT cliente.nome FROM cliente "
                        + "JOIN cliente ON cliente.idcliente = cliente.idcliente",
                "repetida"
        );

        String futura = erroSemantico(
                """
                SELECT cliente.nome FROM cliente
                JOIN status ON status.idstatus = pedido.status_idstatus
                JOIN pedido ON cliente.idcliente = pedido.cliente_idcliente
                """,
                "depois deste JOIN"
        );

        verificar(
                !futura.contains(";"),
                "Mais de um erro para a mesma causa: " + futura
        );

        erroSemantico(
                "SELECT cliente.foo FROM cliente",
                "Coluna foo"
        );

        String repetido = erroSemantico(
                "SELECT foo.nome FROM foo",
                "Tabela não encontrada: foo"
        );

        verificar(
                !repetido.contains(";"),
                "Mensagem repetida: " + repetido
        );

        String semTabela = erroSemantico(
                "SELECT cliente.nome FROM cliente "
                        + "JOIN pedido ON foo = pedido.cliente_idcliente",
                "Coluna não encontrada: foo"
        );

        verificar(
                !semTabela.contains("null"),
                "Mensagem expos valor nulo: " + semTabela
        );

        System.out.println("[APROVADO] Erros semanticos");
    }

    private static void testarMensagensSintaticas() {
        String like = erroSintatico(
                "SELECT cliente.nome FROM cliente "
                        + "WHERE cliente.nome LIKE 'x'"
        );

        verificar(
                !like.contains("ON"),
                "Erro do WHERE citou ON: " + like
        );

        verificar(
                erroSintatico(
                        "SELECT cliente.nome FROM cliente "
                                + "WHERE cliente.nome = 'x' OR cliente.nome = 'y'"
                ).contains("OR"),
                "Mensagem nao indicou o trecho invalido"
        );

        erroSintatico("SELECT cliente.nome FROM cliente JOIN pedido");
        erroSintatico("SELECT cliente.nome cliente");
        erroSintatico("SELECT cliente.nome FROM cliente WHERE (cliente.nome = 'x'");

        System.out.println("[APROVADO] Mensagens sintaticas");
    }

    private static void testarHeuristicasDoPlano() {
        NoOperador original = arvoreOriginal(OITO_TABELAS);

        RastreadorHeuristicas.Resultado resultado =
                new RastreadorHeuristicas().rastrear(original);

        List<PassoExecucao> passos = new GeradorPlanoExecucao()
                .gerarComHeuristicas(resultado)
                .getPassos();

        Set<String> paraSelecao = Set.of(
                "Decomposicao de selecoes", "Antecipacao de selecoes"
        );
        Set<String> paraJuncao = Set.of(
                "Eliminacao de produto cartesiano", "Priorizacao de juncoes"
        );
        Set<String> paraProjecao = Set.of(
                "Reducao de atributos", "Simplificacao de projecoes"
        );

        boolean juncaoPriorizada = false;

        for (PassoExecucao passo : passos) {
            Set<String> permitidas = switch (passo.getTipo()) {
                case SELECAO -> paraSelecao;
                case JUNCAO -> paraJuncao;
                case PROJECAO -> paraProjecao;
                default -> Set.of();
            };

            verificar(
                    permitidas.containsAll(passo.getHeuristicas()),
                    "Heuristica no operador errado: "
                            + passo.getDescricao()
                            + " " + passo.getHeuristicas()
            );

            if (passo.getTipo() == TipoOperador.SELECAO) {
                verificar(
                        passo.getHeuristicas().contains(
                                "Antecipacao de selecoes"),
                        "Selecao sem antecipacao: " + passo.getDescricao()
                );
            }

            juncaoPriorizada |= passo.getHeuristicas()
                    .contains("Priorizacao de juncoes");
        }

        verificar(juncaoPriorizada, "Nenhuma juncao recebeu a priorizacao");

        verificar(
                passos.stream()
                        .filter(p -> p.getTipo() == TipoOperador.PROJECAO)
                        .anyMatch(p -> p.getHeuristicas()
                                .contains("Reducao de atributos")),
                "Nenhuma projecao recebeu a reducao de atributos"
        );

        System.out.println("[APROVADO] Heuristicas do plano");
    }

    private static String algebra(String sql) {
        QuerryObj consulta = new Parser().readRaw(sql);

        return new RelationalFormatter().format(
                new RelationalAlgebraConverter().convert(consulta)
        );
    }

    private static NoOperador arvoreOriginal(String sql) {
        QuerryObj consulta = new Parser().readRaw(sql);

        return new AdaptadorAlgebra().converter(
                new RelationalAlgebraConverter().convert(consulta)
        );
    }

    private static NoOperador otimizar(String sql) {
        NoOperador otimizada = new RastreadorHeuristicas()
                .rastrear(arvoreOriginal(sql))
                .getArvoreOtimizada();

        new AdaptadorGrafico().converter(otimizada);

        return otimizada;
    }

    private static boolean projecaoAbaixoDeSelecao(NoOperador no) {
        if (no == null) {
            return false;
        }

        if (no.getTipo() == TipoOperador.SELECAO
                && no.getEsquerdo().getTipo() == TipoOperador.PROJECAO) {
            return true;
        }

        return projecaoAbaixoDeSelecao(no.getEsquerdo())
                || projecaoAbaixoDeSelecao(no.getDireito());
    }

    private static int selecaoAcimaDeJuncao(NoOperador no) {
        if (no == null) {
            return 0;
        }

        int atual = no.getTipo() == TipoOperador.SELECAO
                && contar(no, TipoOperador.JUNCAO) > 0 ? 1 : 0;

        return atual + selecaoAcimaDeJuncao(no.getEsquerdo())
                + selecaoAcimaDeJuncao(no.getDireito());
    }

    // Ordem de execucao: a juncao mais profunda vem primeiro.
    private static List<String> juncoesNaOrdem(NoOperador no) {
        List<String> descendo = new ArrayList<>();
        coletar(no, TipoOperador.JUNCAO, descendo);

        List<String> ordem = new ArrayList<>();

        for (int i = descendo.size() - 1; i >= 0; i--) {
            ordem.add(descendo.get(i));
        }

        return ordem;
    }

    private static int contar(NoOperador no, TipoOperador tipo) {
        List<String> valores = new ArrayList<>();
        coletar(no, tipo, valores);
        return valores.size();
    }

    private static void coletar(
            NoOperador no,
            TipoOperador tipo,
            List<String> valores
    ) {
        if (no == null) {
            return;
        }

        if (no.getTipo() == tipo) {
            valores.add(no.getValor());
        }

        coletar(no.getEsquerdo(), tipo, valores);
        coletar(no.getDireito(), tipo, valores);
    }

    private static String erroSemantico(String sql, String trecho) {
        try {
            new Parser().readRaw(sql);
        } catch (SemanticException erro) {
            verificar(
                    erro.getMessage().contains(trecho),
                    "Mensagem inesperada: " + erro.getMessage()
            );
            return erro.getMessage();
        }

        throw new AssertionError("Deveria ser erro semantico: " + sql);
    }

    private static String erroSintatico(String sql) {
        try {
            new Parser().readRaw(sql);
        } catch (SyntaxException erro) {
            verificacoes++;
            return erro.getMessage();
        }

        throw new AssertionError("Deveria ser erro sintatico: " + sql);
    }

    private static void verificar(boolean condicao, String mensagem) {
        verificacoes++;

        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }
}
