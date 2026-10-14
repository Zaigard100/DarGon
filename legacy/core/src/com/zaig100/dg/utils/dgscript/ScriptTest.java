package com.zaig100.dg.utils.dgscript;

import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.visitors.AsignValid;
import com.zaig100.dg.utils.dgscript.visitors.FuntionAdder;
import com.zaig100.dg.utils.dgscript.visitors.UsingInit;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.Optimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class ScriptTest {

    static Statement program;
    static List<Token> tokens;
    static long timeT, timeP, timeS;

    @SuppressWarnings("NewApi")
    public static void main(String[] args) throws IOException {
        final String input = new String(Files.readAllBytes(Paths.get("testscript.dgs")));

        timeT = System.currentTimeMillis();
        tokens = (new Lexer(input)).tokenize();
        timeT = System.currentTimeMillis() - timeT;

        timeP = System.currentTimeMillis();
        program = new Parser(tokens).parse();
        timeP = System.currentTimeMillis() - timeP;

        debag();

        program.accept(new UsingInit());
        program.accept(new AsignValid());
        program.accept(new FuntionAdder());
        program = Optimizer.optimize(program);

        statement_show();

        System.out.println("Script run:");
        timeS = System.currentTimeMillis();
        program.execute();
        timeS = System.currentTimeMillis() - timeS;
        System.out.println("Script end (" + timeS + "millisec)");

    }

    static void debag() {
        System.out.println("Lexing (" + timeT + " millisec) : ");
        token_show();

        System.out.println("Parsing ( " + timeP + " millisec) : ");
        statement_show();
    }

    static void token_show() {
        for (Token token : tokens) {
            System.out.print(tokens.indexOf(token) + ": ");
            System.out.println(token.toString());
        }
    }

    static void statement_show() {
        System.out.println(program.toString());

    }

}
