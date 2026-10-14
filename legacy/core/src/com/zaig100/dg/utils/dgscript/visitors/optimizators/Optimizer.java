package com.zaig100.dg.utils.dgscript.visitors.optimizators;

import com.zaig100.dg.utils.dgscript.ast.Statement;

public class Optimizer {
    public interface Info {
        public int optimizCount();

        public String sumInfo();
    }

    public static Statement optimize(Statement st) {
        int iterOptiCount = 0, level = 1;
        final ConstantFolding cF = new ConstantFolding();
        final DeadCodeElimitation dcE = new DeadCodeElimitation();
        final ExpressionSimplification eS = new ExpressionSimplification();
        Statement result = st;
        do {
            level++;
            iterOptiCount = cF.optimizCount() + dcE.optimizCount() + eS.optimizCount();
            result = (Statement) result.accept(cF, null);
            result = (Statement) result.accept(dcE, null);
            result = (Statement) result.accept(eS, null);
        } while (((cF.optimizCount() + dcE.optimizCount() + eS.optimizCount()) - iterOptiCount) > 0);
        System.out.println("Levels: " + level);
        System.out.println(cF.sumInfo());
        System.out.println(dcE.sumInfo());
        System.out.println(eS.sumInfo());
        return result;
    }

}
