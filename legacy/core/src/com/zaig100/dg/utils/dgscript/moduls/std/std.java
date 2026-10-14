package com.zaig100.dg.utils.dgscript.moduls.std;

import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.moduls.Module;

public class std implements Module {
    @Override
    public void init() {
        Functions.set("echo", new echo());
        Functions.set("array", new array());
        Functions.set("sleep", new sleep());
        Functions.set("thread", new thread());
        Functions.set("rand", new rand());
        Functions.set("round", new round());
    }
}
