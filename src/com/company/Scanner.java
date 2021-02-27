package com.company;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// класс сканера
public class Scanner {

    // поля сканнера
    private String SrsModule;       // исходный модуль
    private int Position;           // позиция
    private int Line;               // номер строки
    private final String ErrorFileName;   // путь к файлу с ошибками
    // критические границы
    private final int MaxSrsModuleSize;
    private final int MaxLexSize;

    // конструктор сканнера, инициализирующий исходный модуль со стандартной инициализацией критических значений
    public Scanner(String SrsModuleFileName, String ErrorFileName) throws Exception {
        MaxSrsModuleSize = 10000;
        MaxLexSize = 20;
        File old = new File(ErrorFileName);
        old.delete();
        this.ErrorFileName = ErrorFileName;
        Position = 0;
        Line = 1;
        GetData(SrsModuleFileName);
    }

    // чтение исходного модуля
    public void GetData(String SrsModuleFile) throws Exception {
        try {
            SrsModule = Files.readString(Paths.get(SrsModuleFile));
        } catch (IOException ioEX) {
            WriteError("bad source module file reading: " + ioEX.toString());
        }
        if (SrsModule.length() > MaxSrsModuleSize) {
            WriteError("too long source module size");
            throw new Exception("Bad source module, See errors log");
        }
        SrsModule = SrsModule.replaceAll("\r", "");
        SrsModule += "\0\0";
    }

    // запись ошибки
    private void WriteError(String msg) throws IOException {
        FileWriter ErrorFile = new FileWriter(ErrorFileName, true);
        ErrorFile.write("error: " + msg + " ; position: " + String.valueOf(Position) + "; line: " + String.valueOf(Line) + "\n");
        ErrorFile.close();
    }

    // сканирование очередной лексемы
    public Lexeme Scan() throws IOException {

        // 1. пропустить игнорируемые символы
        while (SrsModule.charAt(Position) == ' ' || SrsModule.charAt(Position) == '\n' || SrsModule.charAt(Position) == '\t' ||
                (SrsModule.charAt(Position) == '/' && (SrsModule.charAt(Position+1) == '/' || SrsModule.charAt(Position+1) == '*'))) {
            switch (SrsModule.charAt(Position)) {
                case ' ', '\t':
                    Position++;
                    break;
                case '\n':
                    Position++;
                    Line++;
                    break;
                case '/': {   // comment
                    Position++;

                    if (SrsModule.charAt(Position) == '/') {
                        while (SrsModule.charAt(Position) != '\n' && SrsModule.charAt(Position) != '\0')
                            Position++;
                        Position++;
                        Line++;
                    } else {            // multiline comment
                        Position++;     // eat '*'
                        char s = SrsModule.charAt(Position);
                        while(SrsModule.charAt(Position) != '\0' && !(SrsModule.charAt(Position) == '*' && SrsModule.charAt(Position+1) == '/')) {
                            if (SrsModule.charAt(Position) == '\n')
                                Line++;
                            Position++;
                        }
                        if (SrsModule.charAt(Position) == '\0') {
                            WriteError("unclosed multiline comment");
                            return new Lexeme(Err, "/*");
                        } else
                            Position += 2; // eat */
                    }
                }
                break;
            }
        }

        // 2 выделить лексему
        String lex = "";

        // 1. если латинсая буква
        char symb = SrsModule.charAt(Position);
        if ((symb >= 'a' && symb <= 'z') || (symb >= 'A' && symb <= 'Z') || symb == '_') {
            while ((symb >= 'a' && symb <= 'z') || (symb >= 'A' && symb <= 'Z') || symb == '_' || (symb >= '0' && symb <= '9')) {
                Position++;
                lex += symb;
                if (lex.length() > MaxLexSize) {
                    WriteError("too long lexeme");
                    return new Lexeme(Err, lex);
                }
                symb = SrsModule.charAt(Position);  // update current symbol
            }

            // определить, был ли идентификатор или ключевое слово
            switch(lex) {
                case "main": return new Lexeme(Main, lex);
                case "int": return new Lexeme(Int, lex);
                case "short": return new Lexeme(Short, lex);
                case "long": return new Lexeme(Long, lex);
                case "double": return new Lexeme(Double, lex);
                case "for": return new Lexeme(For, lex);
                case "const": return new Lexeme(Const, lex);
                default: return new Lexeme(Id, lex);   // если не ключевое, то идентификатор
            }
        }

        // 2. если цифра
        if (symb >= '0' && symb <= '9') {
            while (symb >= '0' && symb <= '9') {
                Position++;
                lex += symb;
                if (lex.length() > MaxLexSize) {
                    WriteError("too long lexeme");
                    return new Lexeme(Err, lex);
                }
                symb = SrsModule.charAt(Position);
            }
            return new Lexeme(IntConst, lex);
        }

        // 3. остальные символы
        if (symb == '\'') {
            lex += symb;
            Position++;
            symb = SrsModule.charAt(Position);
            if (symb == '\\') {
                lex += symb;
                Position++;
                symb = SrsModule.charAt(Position);
            }
            lex += symb;    //иначе это простой символ
            Position++;     //Ожидается закрывающаяся кавычка
            symb = SrsModule.charAt(Position);
            if (symb != '\''){
                WriteError("invalid lexeme '" + lex + "'");
                return new Lexeme(Err, lex);
            }
            lex += symb;
            Position++;
            return new Lexeme(CharConst,lex);
        }

        if (symb == '=') {
            lex += symb;
            Position++;
            symb = SrsModule.charAt(Position);
            if (symb == '=') {
                lex += symb;
                Position++;
                return new Lexeme(Equ, lex);
            } else
                return new Lexeme(Assignment, lex);
        }
        if (symb == '!') {
            lex += symb;
            Position++;
            symb = SrsModule.charAt(Position);
            if (symb == '=') {
                lex += symb;
                Position++;
                return new Lexeme(Not_equ, lex);
            } else {
                WriteError("invalid lexeme '" + lex + "'");
                return new Lexeme(Err, lex);
            }
        }
        if (symb == '<') {
            lex += symb;
            Position++;
            symb = SrsModule.charAt(Position);
            if (symb == '=') {
                lex += symb;
                Position++;
                return new Lexeme(LessEqu, lex);
            } else
                return new Lexeme(Less, lex);
        }
        if (symb == '>') {
            lex += symb;
            Position++;
            symb = SrsModule.charAt(Position);
            if (symb == '=') {
                lex += symb;
                Position++;
                return new Lexeme(MoreEqu, lex);
            } else
                return new Lexeme(More, lex);
        }
        if (symb == '+') {
            lex += symb;
            Position++;
            return new Lexeme(Plus, lex);
        }
        if (symb == '-') {
            lex += symb;
            Position++;
            return new Lexeme(Minus, lex);
        }
        if (symb == '*') {
            lex += symb;
            Position++;
            return new Lexeme(Mul, lex);
        }
        if (symb == '/') {
            lex += symb;
            Position++;
            return new Lexeme(Div, lex);
        }
        if (symb == '%') {
            lex += symb;
            Position++;
            return new Lexeme(Mod, lex);
        }
        if (symb == ',') {
            lex += symb;
            Position++;
            return new Lexeme(Comma, lex);
        }
        if (symb == ';') {
            lex += symb;
            Position++;
            return new Lexeme(Semicolon, lex);
        }
        if (symb == '{') {
            lex += symb;
            Position++;
            return new Lexeme(OpeningBrace, lex);
        }
        if (symb == '}') {
            lex += symb;
            Position++;
            return new Lexeme(ClosingBrace, lex);
        }
        if (symb == '(') {
            lex += symb;
            Position++;
            return new Lexeme(OpeningBracket, lex);
        }
        if (symb == ')') {
            lex += symb;
            Position++;
            return new Lexeme(ClosingBracket, lex);
        }
        if (symb == '[') {
            lex += symb;
            Position++;
            return new Lexeme(OpeningSquareBracket, lex);
        }
        if (symb == ']') {
            lex += symb;
            Position++;
            return new Lexeme(ClosingSquareBracket, lex);
        }

        // конечный символ
        if (symb == '\0')
            return new Lexeme(End, "null_char");

        // else in the end error
        lex += symb;
        WriteError("invalid lexeme '" + lex + "'");
        Position++;
        return new Lexeme(Err, lex);
    }

    public static int Id = 1;
    public static int IntConst = 2;
    public static int CharConst = 3;
    public static int Assignment = 4;

    public static int End = 5;
    public static int Err = 6;

    public static int Main = 7;
    public static int Int = 8;
    public static int Short = 9;
    public static int Long = 10;
    public static int Double = 11;
    public static int For = 12;
    public static int Const = 13;

    public static int Semicolon = 14;
    public static int Comma = 15;
    public static int OpeningBracket = 16;
    public static int ClosingBracket = 17;
    public static int OpeningBrace = 18;
    public static int ClosingBrace = 19;
    public static int OpeningSquareBracket = 20;
    public static int ClosingSquareBracket = 21;

    public static int Plus = 22;
    public static int Minus = 23;
    public static int Mul = 24;
    public static int Div = 25;
    public static int Mod = 26;
    public static int Equ = 27;
    public static int LessEqu = 28;
    public static int MoreEqu = 29;
    public static int Less = 30;
    public static int More = 31;
    public static int Not_equ = 32;
}
