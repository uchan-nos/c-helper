package com.github.uchan_nos.c_helper.pointer;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * 解析の途中で検出した問題を表す.
 * 例えばfreeに未初期化変数を渡しているとか、freeにmallocした以外の値を渡しているなど
 */
public class MemoryProblem {
    public static enum Kind {
        FREE_INVALID_NUMBER_ARGUMENT, FREE_INVALID_ARGUMENT,
        DOUBLE_FREE, UNINITIALIZED_VALUE_FREE, UNKNOWN_VALUE_FREE,
        REALLOC_INVALID_NUMBER_ARGUMENT, REALLOC_INVALID_ARGUMENT,
        UNINITIALIZED_VALUE_REALLOC, UNKNOWN_VALUE_REALLOC
    }

    public final IASTNode position;
    public final Kind message;
    public MemoryProblem(IASTNode position, Kind message) {
        this.position = position;
        this.message = message;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MemoryProblem)) {
            return false;
        }
        MemoryProblem p = (MemoryProblem) o;
        return Util.equalsOrBothNull(position, p.position)
            && Util.equalsOrBothNull(message, p.message);
    }

    @Override
    public final int hashCode() {
        int result = 17;
        result = 31 * result + (position == null ? 0 : position.hashCode());
        result = 31 * result + (message == null ? 0 : message.hashCode());
        return result;
    }
}
