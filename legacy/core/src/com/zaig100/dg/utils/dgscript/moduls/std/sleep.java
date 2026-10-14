package com.zaig100.dg.utils.dgscript.moduls.std;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class sleep implements Function {

    @Override
    public Value execute(Value... args) {
        try {
            Thread.sleep((long) args[0].asNum());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return NumberVal.ZERO;
    }
}
