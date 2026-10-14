package com.zaig100.dg.utils.dgscript.moduls.dargon;

import com.zaig100.dg.utils.contain.DoorC;
import com.zaig100.dg.utils.contain.ItemC;
import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.DGObjectValue;
import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.StringVal;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.objects.Obj;

public class getValueInObj implements Function {
    @Override
    public Value execute(Value... args) {
        DGObjectValue arrVal;
        StringVal valName;
        if (args.length == 3) {
            if (args[0] instanceof DGObjectValue) {
                arrVal = (DGObjectValue) args[0];
                valName = (StringVal) args[1];
            } else if (args[0] instanceof StringVal) {
                arrVal = (DGObjectValue) args[1];
                valName = (StringVal) args[0];
            } else {
                throw new RuntimeException("Invalid value type");
            }
            return ((Obj)(arrVal.raw())).getVal(valName.asString());
        }
        return null;
    }

}
