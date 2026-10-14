package com.zaig100.dg.utils.dgscript.moduls.dargon;

import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.DGObjectValue;
import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.objects.Obj;

public class setValueInObj implements Function {
    @Override
    public Value execute(Value... args) {

        DGObjectValue arr = null;
        StringVal name = null;
        Value val = null;

        if (args.length == 3) {
            arr = (DGObjectValue) args[0];
            name = (StringVal) args[1];
            val = args[2];

            try {
                ((Obj) (arr.raw())).setVal(name.asString(), val);
                return NumberVal.fromBoolean(true);
            } catch (RuntimeException e) {
                return NumberVal.fromBoolean(false);
            }
        }
        return NumberVal.fromBoolean(false);
    }



}
