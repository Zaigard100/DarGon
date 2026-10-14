package com.zaig100.dg.utils.dgscript.moduls.math;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class abs implements Function {
    @Override
    public Value execute(Value... args) {
        if (args.length != 1) throw new RuntimeException("Argument count exeption");
        return new NumberVal(Math.abs(args[0].asNum()));
    }
}
