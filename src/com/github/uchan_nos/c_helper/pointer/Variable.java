package com.github.uchan_nos.c_helper.pointer;

import org.eclipse.cdt.core.dom.ast.IASTName;

/**
 * 1つの変数を表す不変オブジェクト.
 */
public class Variable {
    public enum States {
        UNDEFINED,
        NULL,
        POINTING
    }

    private final IASTName name;
    private final States status;
    private final Address value;

    public Variable(IASTName name, States status, Address value) {
        this.name = name;
        this.status = status;
        this.value = value;
    }

    public IASTName name() {
        return name;
    }

    public States status() {
        return status;
    }

    public Address value() {
        return value;
    }
}
