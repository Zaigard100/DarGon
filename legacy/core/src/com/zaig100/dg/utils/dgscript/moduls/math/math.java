package com.zaig100.dg.utils.dgscript.moduls.math;

import com.zaig100.dg.utils.dgscript.lib.Functions;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Variables;
import com.zaig100.dg.utils.dgscript.moduls.Module;

public class math implements Module {


    @Override
    public void init() {
        Functions.set("abs", new abs());
        Functions.set("cos", new cos());
        Functions.set("sin", new sin());
        Functions.set("sqrt", new sqrt());
        Functions.set("toDegrees", new toDegrees());
        Functions.set("toRadians", new toRadians());

        Variables.set("S_PI", new NumberVal(3.14));
        Variables.set("PI", new NumberVal(Math.PI));
        Variables.set("S_E", new NumberVal(2.72));
        Variables.set("E", new NumberVal(Math.E));
        Variables.set("G_R", new NumberVal(1.618));

    }



}
