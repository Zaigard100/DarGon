package com.zaig100.dg.utils.dgscript.ast;

import com.zaig100.dg.utils.dgscript.lib.Value;

public interface Expression extends Node {
    Value eval();
}
