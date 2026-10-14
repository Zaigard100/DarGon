package com.zaig100.dg.utils.dgscript.moduls.std;


import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class echo implements Function {
    @Override
    public Value execute(Value... args) {
        for (Value val : args) {
            System.out.print(val.asString() + " ");
        }
        System.out.println();
        return NumberVal.ZERO;
    }
}
