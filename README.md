# Processador de Consultas

Trabalho de Banco de Dados: recebe uma consulta SQL, valida a sintaxe e o
Modelo Relacional, converte para álgebra relacional, aplica as heurísticas de
otimização e exibe os grafos e o plano de execução.

## Como executar

No Windows, dê dois cliques em `Executar.bat`. É preciso ter o JDK 17 ou
superior instalado.

Em outro sistema, ou pelo terminal:

```
javac -encoding UTF-8 -d out/app -sourcepath src src/UI/MainWindow.java
java -cp out/app UI.MainWindow
```

## SQL aceito

- Comandos: `SELECT`, `FROM`, `JOIN`, `ON`, `WHERE`.
- Operadores: `=`, `>`, `<`, `<=`, `>=`, `<>`, `AND` e parênteses.
- Cada `JOIN` precisa ligar a tabela nova a uma tabela anterior no `ON`.
- Maiúsculas, minúsculas e espaços repetidos são ignorados.
- Fora do escopo: `SELECT *`, `OR`, `FROM a, b` e apelidos de tabela.

## Testes

Cada classe de teste é executada pelo próprio `main` e falha com erro em caso
de problema:

```
javac -encoding UTF-8 -d out/testes -sourcepath src src/hu04/teste/*.java src/hu05/testes/*.java
java -cp out/testes hu04.teste.TestesRegressao
```

As demais classes estão em `src/hu04/teste` e `src/hu05/testes`, e o GitHub
Actions executa todas a cada push e pull request.
