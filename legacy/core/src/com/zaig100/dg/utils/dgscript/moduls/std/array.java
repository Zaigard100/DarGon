package com.zaig100.dg.utils.dgscript.moduls.std;

import com.zaig100.dg.utils.dgscript.lib.ArrayValue;
import com.zaig100.dg.utils.dgscript.lib.Function;
import com.zaig100.dg.utils.dgscript.lib.NumberVal;
import com.zaig100.dg.utils.dgscript.lib.Value;

public class array implements Function {
    @Override
    public Value execute(Value... args) {
        return createArr(args, 0);
    }

    private ArrayValue createArr(Value[] args, int index) {
        final int size = (int) args[index].asNum();
        final int last = args.length - 1;
        ArrayValue array = new ArrayValue(size);
        if (index == last) {
            for (int i = 0; i < size; i++) {
                array.set(i, NumberVal.ZERO);
            }
        } else if (index < last) {
            for (int i = 0; i < size; i++) {
                array.set(i, createArr(args, index + 1));
            }
        }
        return array;
    }
}
