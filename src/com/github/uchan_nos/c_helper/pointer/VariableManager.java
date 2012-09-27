package com.github.uchan_nos.c_helper.pointer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IVariable;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * 変数の状態を管理する.
 * 変数は明示的に割り当てられない限り Undefined を示す.
 */
public class VariableManager {
    private final Map<IVariable, Variable> variables;

    public VariableManager() {
        this.variables = new HashMap<IVariable, Variable>();
    }

    public VariableManager(VariableManager o) {
        // Variable が不変オブジェクトなので，シャローコピーする
        this.variables = new HashMap<IVariable, Variable>(o.variables);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof VariableManager)) {
            return false;
        }
        VariableManager m = (VariableManager) o;
        return Util.equalsOrBothNull(this.variables, m.variables);
    }

    @Override
    public final int hashCode() {
        return variables.hashCode();
    }

    /**
     * 指定された変数の状態を返す.
     * もし指定された変数がこのマネージャに登録されていないなら、UNDEFINEDを返す.
     * その変数が既にUNDEFINEDの状態でこのマネージャに登録されているならやはりUNDEFINEDを返すため、
     * このメソッドでその変数が登録されているかどうかは判定できない。
     */
    public Variable.States getVariableStatus(IVariable v) {
        Variable var = variables.get(v);
        if (var == null) {
            return Variable.States.UNDEFINED;
        } else {
            return var.status();
        }
    }

    /**
     * 指定されたバインディングを持つ変数を返す.
     * 登録されていないなら null を返す.
     */
    public Variable get(IVariable v) {
        return variables.get(v);
    }

    /**
     * 指定された変数を登録する.
     * もし指定された変数が既にこのマネージャに登録されているなら、上書きする.
     */
    public void put(Variable var) {
        variables.put(var.binding(), var);
    }

    /**
     * このマネージャが保持している変数の一覧を返す.
     */
    public Collection<Variable> getContainingVariables() {
        return variables.values();
    }
}
