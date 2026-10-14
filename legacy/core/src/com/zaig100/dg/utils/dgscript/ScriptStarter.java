package com.zaig100.dg.utils.dgscript;

import com.zaig100.dg.screen.game.LevelModScreen;
import com.zaig100.dg.utils.dgscript.ast.Statement;
import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.lib.Variables;
import com.zaig100.dg.utils.dgscript.visitors.AsignValid;
import com.zaig100.dg.utils.dgscript.visitors.FuntionAdder;
import com.zaig100.dg.utils.dgscript.visitors.UsingInit;
import com.zaig100.dg.world.objects.Obj;

import java.util.ArrayList;
import java.util.List;

public class ScriptStarter {
    public static LevelModScreen ps;
    Thread threads;
    Statement program;
    List<Token> tokens;
    public boolean isSetObj = false;
     ArrayList<Obj> objects;


    public  void load(String code, LevelModScreen ps) {

        Functions.clear();
        Variables.clear();

        ScriptStarter.ps = ps;

        tokens = (new Lexer(code)).tokenize();

        program = new Parser(tokens).parse();

        program.accept(new UsingInit());
        program.accept(new AsignValid());
        program.accept(new FuntionAdder());

        threads = new Thread() {
            @Override
            public void run() {
                super.run();
                program.execute();
            }
        };
    }

    public  void start() {
        threads.start();
    }

    public  void setObj(Obj object) {
        objects.add(object);
        isSetObj = true;
    }

    public ArrayList<Obj> setObjs() {
        return objects;
    }

    public  void clear() {
        isSetObj = true;
        objects.clear();
    }

}
