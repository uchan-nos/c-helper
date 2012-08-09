package com.github.uchan_nos.c_helper.analysis.values;

import org.eclipse.cdt.core.dom.ast.IType;

/**
 * C言語プログラムの様々な値を表す.
 * @author uchan
 */
public abstract class Value {
    public static final int OVERFLOWED = 1 << 0;
    public static final int UNDEFINED = 1 << 1;
    public static final int IMPLDEPENDENT = 1 << 2;

    /**
     * このValueが表す型を返す.
     * @return
     */
    public abstract IType getType();

    /**
     * このValueの状態を返す.
     * @return OVERFLOWED, UNDEFINED, IMPLDEPENDENTのいずれか
     */
    public abstract int getFlag();

    /**
     * 型変換する.
     * @param newType
     * @return
     */
    public abstract Value castTo(IType newType);

    /**
     * このValueが真値を表す場合はtrueを返す.
     * @return Valueが非0の値を保持しているならtrue、0の値を保持しているならfalse
     */
    public abstract boolean isTrue();
}
