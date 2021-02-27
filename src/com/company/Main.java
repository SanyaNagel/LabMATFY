package com.company;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("Good test:");
        Scanner scanner1 = new Scanner("good_test.c","error.data");
        Lexeme lexeme = new Lexeme(-2,"");
        while(lexeme.type != Scanner.End) {
            lexeme = scanner1.Scan();
            System.out.println(lexeme.image + " type " + lexeme.type);
        }

        System.out.println("_______________");

        System.out.println("Bad test:");
        Scanner scanner2 = new Scanner("bad_test.c","error.data");

        lexeme = new Lexeme(-2,"");
        while(lexeme.type != Scanner.End) {
            lexeme = scanner2.Scan();
            System.out.println(lexeme.image + " type " + lexeme.type);
        }
    }
}
