package com.github.uchan_nos.c_helper.pointer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.cdt.core.dom.ast.IASTName;

public class MemoryStatus {
    private final Map<IASTName, Variable> variables;
    private final MemoryManager memoryManager;

    public MemoryStatus(Collection<IASTName> variableNames) {
        this.variables = new HashMap<IASTName, Variable>();
        this.memoryManager = new MemoryManager();

        for (IASTName n : variableNames) {
            this.variables.put(n, null);
        }
    }

    public MemoryStatus(MemoryStatus o) {
        // Variable が不変オブジェクトなので，シャローコピーする
        this.variables = new HashMap<IASTName, Variable>(o.variables);

        this.memoryManager = new MemoryManager(o.memoryManager);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MemoryStatus)) {
            return false;
        }
        MemoryStatus s = (MemoryStatus) o;
        return this.variables.equals(s.variables)
            && this.memoryManager.equals(s.memoryManager);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.variables.hashCode();
        result = 31 * result + this.memoryManager.hashCode();
        return result;
    }

    public void update(Variable... vs) {
        for (Variable v : vs) {
            if (!this.variables.containsKey(v.name())) {
                throw new NoSuchElementException();
            }
            this.variables.put(v.name(), v);
        }
    }

    public Map<IASTName, Variable> variables() {
        return this.variables;
    }

    public MemoryManager memoryManager() {
        return this.memoryManager;
    }
}
