package com.zaig100.dg.utils.dgscript.lib;

public class Types {
    public static final int NUM = 0, STR = 1, ARR = 2, FUNC = 3,OBJ = 4;
    private static final String[] NAMES = {"num", "str", "func"};

    public static String typeToString(int type) {
        if (0 <= type && type <= 4) {
            return NAMES[type];
        }
        return "unknown";
    }
}
