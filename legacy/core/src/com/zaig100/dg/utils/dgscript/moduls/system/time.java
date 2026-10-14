package com.zaig100.dg.utils.dgscript.moduls.system;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class time implements Function {
    @Override
    public Value execute(Value... args) {
        return new NumberVal(System.currentTimeMillis());

    }
}
