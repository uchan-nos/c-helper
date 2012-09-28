package com.github.uchan_nos.c_helper.pointer;

import org.eclipse.cdt.core.dom.ast.IVariable;

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

    private final IVariable binding;
    private final States status;
    private final Address value;

    public Variable(IVariable binding, States status, Address value) {
        this.binding = binding;
        this.status = status;
        this.value = value;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Variable)) {
            return false;
        }
        Variable v = (Variable) o;
        return Util.equalsOrBothNull(this.binding, v.binding)
            && this.status == v.status
            && Util.equalsOrBothNull(this.value, v.value);
    }

    @Override
    public final int hashCode() {
        int result = 17;
        result = 31 * result + (binding == null ? 0 : binding.hashCode());
        result = 31 * result + status.ordinal();
        result = 31 * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VAR(");
        sb.append(String.valueOf(binding));
        sb.append(',');
        sb.append(String.valueOf(status));
        sb.append(',');
        sb.append(String.valueOf(value));
        sb.append(')');
        return sb.toString();
    }

    public IVariable binding() {
        return binding;
    }

    public States status() {
        return status;
    }

    public Address value() {
        return value;
    }
}
