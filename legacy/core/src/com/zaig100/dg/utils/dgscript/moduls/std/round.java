package com.zaig100.dg.utils.dgscript.moduls.std;

import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class round implements Function {

    @Override
    public Value execute(Value... args) {
        if (args.length == 1) return new NumberVal(Math.round(args[0].asNum()));
        if (args.length == 2) return new NumberVal(round(args[0].asNum(), (int) args[1].asNum()));
        else throw new RuntimeException("Argument count exeption");
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
