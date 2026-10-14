package com.zaig100.dg.utils.dgscript.moduls.std;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class rand implements Function {
    public Value execute(Value... args) {
        if (args.length == 0) return new NumberVal(Math.random());
        else if (args.length == 1) return new NumberVal(Math.random() * args[0].asNum());
        else if (args.length == 2)
            return new NumberVal((Math.random() * (args[1].asNum() - args[0].asNum())) + args[0].asNum());
        else throw new RuntimeException("Argument count exeption");
    }
}
