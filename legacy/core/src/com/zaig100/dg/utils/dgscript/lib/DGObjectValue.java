package com.zaig100.dg.utils.dgscript.lib;

import com.zaig100.dg.utils.contain.ObjC;
import com.zaig100.dg.world.objects.Obj;

public class DGObjectValue implements Value {
    Obj object;
    Obj.ObjType type;

    public DGObjectValue(Obj object, Obj.ObjType type){
        this.object = object;
        this.type = type;
    }

    @Override
    public Object raw() {
        return object;
    }

    @Override
    public int asInt() {
        throw new RuntimeException("Cannot cast function to number");
    }

    @Override
    public double asNum() {
        throw new RuntimeException("Cannot cast function to number");
    }

    @Override
    public String asString() {
        return object.toString();
    }

    @Override
    public int type() {
        return Types.OBJ;
    }

    public Obj getObject() {
        return object;
    }

    public Obj.ObjType getType() {
        return type;
    }
}
