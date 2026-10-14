package com.zaig100.dg.utils.dgscript.moduls.math;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class sin implements Function {
    @Override
    public Value execute(Value... args) {
        if (args.length != 1) throw new RuntimeException("Argument count exeption");
        return new NumberVal(Math.sin(args[0].asNum()));
    }
}
