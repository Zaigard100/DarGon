package com.zaig100.dg.utils.dgscript.ast;

import com.zaig100.dg.utils.dgscript.visitors.Visitor;
import com.zaig100.dg.utils.dgscript.visitors.optimizators.ResultVisitor;

public interface Node {

    void accept(Visitor visitor);

    <R, T> R accept(ResultVisitor<R, T> visitor, T input);
}
