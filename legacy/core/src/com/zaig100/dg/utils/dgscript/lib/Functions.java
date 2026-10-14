package com.zaig100.dg.utils.dgscript.lib;

import java.util.HashMap;
import java.util.Map;

import static com.zaig100.dg.utils.dgscript.lib.NumberVal.ZERO;

public class Functions {

    private static Map<String, Function> fuctions = new HashMap<>();

    static {
        clear();
    }

    public static void clear() {
        fuctions.clear();
        //print
        fuctions.put("print", new Function() {
            @Override
            public Value execute(Value... args) {
                if (args.length != 1) throw new RuntimeException("Argument count exeption");
                System.out.print(args[0].asString());
                return ZERO;
            }
        });
        //println
        fuctions.put("println", new Function() {
            @Override
            public Value execute(Value... args) {
                if (args.length == 0) System.out.println();
                else if (args.length == 1) System.out.println(args[0].asString());
                else throw new RuntimeException("Argument count exeption");
                return ZERO;
            }
        });
    }

    public static boolean isExist(String key) {
        return fuctions.containsKey(key);
    }

    public static Function get(String key) {
        if (!isExist(key)) throw new RuntimeException("Unknowm function: " + key);
        return fuctions.get(key);
    }

    public static void set(String key, Function func) {
        if (isExist(key)) throw new RuntimeException("Function duplikat: " + key);
        fuctions.put(key, func);
    }

}
