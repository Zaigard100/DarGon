package com.zaig100.dg.utils.dgscript.moduls.dargon;

import com.zaig100.dg.utils.contain.ObjC;
import com.zaig100.dg.utils.dgscript.lib.DGObjectValue;
import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.moduls.Module;
import com.zaig100.dg.world.objects.Obj;

public class dargon implements Module {
    @Override
    public void init() {
        Functions.set("getObj", new getObj());
        Functions.set("getValueInObj", new getValueInObj());
        Functions.set("setValueInObj", new setValueInObj());
    }

    static DGObjectValue objInValue(Obj obj, Obj.ObjType type) {
       return new DGObjectValue(obj,type);
    }

}
