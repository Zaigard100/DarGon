package com.zaig100.dg.utils.dgscript.exeptions;

public class OperationIsNotSupportedExeption extends RuntimeException {
    public OperationIsNotSupportedExeption(Object op) {
        super("Operation " + op + " is not supported");
    }

    public OperationIsNotSupportedExeption(Object op, String msg) {
        super("Operation " + op + " is not supported " + msg);
    }
}
