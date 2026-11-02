import Parser.Parser;
import Parser.QuerryObj;


void main() {
//    String Exemplo1 = "Select Row1, Row2 From Table1";
//    System.out.println("Exemplo 1: " + Exemplo1);
//    Parser.QuerryObj resultado = Parser.Parser.readRaw(Exemplo1);
//    System.out.println(resultado);


//    String Exemplo2 = "Select Row1, Row2 From Table1 Where index > 0";
//    System.out.println("Exemplo 2: " + Exemplo2);
//    QuerryObj resultado = Parser.readRaw(Exemplo2);
//    System.out.println(resultado);

    String Exemplo3 = "Select Descricao From Categoria Where index > 0";
    System.out.println("Exemplo 3: " + Exemplo3);
    QuerryObj resultado = Parser.readRaw(Exemplo3);
    System.out.println(resultado);
}
