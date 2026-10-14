package com.zaig100.dg.utils.dgscript.moduls.std;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class thread implements Function {

    @Override
    public Value execute(final Value... args) {
        if (args.length == 1) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    Functions.get(args[0].asString()).execute();
                }
            }.start();
            return NumberVal.ZERO;
        } else {
            throw new RuntimeException("Argument count exeption");
        }
    }
}
