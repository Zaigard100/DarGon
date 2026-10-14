package com.zaig100.dg.utils.dgscript.moduls.system;

import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.moduls.Module;

public class system implements Module {
    @Override
    public void init() {
        Functions.set("time", new time());
    }
}
