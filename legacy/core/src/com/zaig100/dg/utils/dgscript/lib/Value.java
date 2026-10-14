package com.zaig100.dg.utils.dgscript.lib;

public interface Value {

    Object raw();

    int asInt();

    double asNum();

    String asString();

    int type();

}
