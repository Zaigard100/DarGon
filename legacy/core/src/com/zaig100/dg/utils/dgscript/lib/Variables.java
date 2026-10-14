package com.zaig100.dg.utils.dgscript.lib;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Variables {
    private static final NumberVal ZERO = new NumberVal(0);
    private static Map<String, Value> variables = new HashMap<>();
    private static Stack<Map<String, Value>> stack = new Stack<>();

    public static void push() {
        stack.push(new HashMap<String, Value>(variables));
    }

    public static void pop() {
        variables = stack.pop();
    }

    static {
        clear();
    }

    public static void clear() {
        stack.clear();
        variables.clear();

    }

    public static boolean isExist(String key) {
        return variables.containsKey(key);
    }

    public static Value get(String key) {
        if (!isExist(key)) return ZERO;
        return variables.get(key);
    }

    public static void set(String key, Value val) {
        variables.put(key, val);
    }

}
