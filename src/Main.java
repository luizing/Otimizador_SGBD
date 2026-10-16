import Parser.Parser;
import Parser.QuerryObj;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
//    String Exemplo1 = "Select Row1, Row2 From Table1";
//    System.out.println("Exemplo 1: " + Exemplo1);
//    Parser.QuerryObj resultado = Parser.Parser.readRaw(Exemplo1);
//    System.out.println(resultado);


    String Exemplo2 = "Select Row1, Row2 From Table1, Where index > 0";
    System.out.println("Exemplo 2: " + Exemplo2);
    QuerryObj resultado = Parser.readRaw(Exemplo2);
    System.out.println(resultado);
}
