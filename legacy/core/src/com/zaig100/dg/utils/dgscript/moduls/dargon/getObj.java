package com.zaig100.dg.utils.dgscript.moduls.dargon;

import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.Value;
import com.zaig100.dg.world.World;
import com.zaig100.dg.world.objects.Obj;

import java.util.ArrayList;

public class getObj implements Function {
    @Override
    public Value execute(Value... args) {

        final ArrayList<Obj> objs = World.map.objectsU;
        objs.addAll(World.map.objectsO);
        objs.addAll(World.map.stair);
        final ArrayList<Obj> cond = new ArrayList<>();
        for (Obj obj : objs) {
            if (obj.getTag().equals((String) args[0].asString())) {
                cond.add(obj);
            }
        }
        if (args.length == 1) {
            ArrayValue objVal = new ArrayValue(cond.size());
            for (int i = 0; i < cond.size(); i++) {
                objVal.set(i, dargon.objInValue(cond.get(i),cond.get(i).type));
            }
            return objVal;
        }
        if (args.length == 2) {
            return dargon.objInValue(cond.get((int) args[1].asNum()),cond.get((int) args[1].asNum()).getType());
        }
        throw new RuntimeException("Argument count exeption");
    }


}
