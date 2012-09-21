package com.github.uchan_nos.c_helper.pointer;

import org.eclipse.cdt.core.dom.ast.IASTName;

import com.github.uchan_nos.c_helper.util.Util;

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

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Variable)) {
            return false;
        }
        Variable v = (Variable) o;
        return Util.equalsOrBothNull(this.name, v.name)
            && this.status == v.status
            && Util.equalsOrBothNull(this.value, v.value);
    }

    @Override
    public final int hashCode() {
        int result = 17;
        result = 31 * result + (name == null ? 0 : name.hashCode());
        result = 31 * result + status.ordinal();
        result = 31 * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(name.getSimpleID());
        sb.append(',');
        sb.append(status.toString());
        sb.append(',');
        sb.append(value.toString());
        sb.append(')');
        return sb.toString();
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
