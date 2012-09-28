package com.github.uchan_nos.c_helper.pointer;

//import org.eclipse.cdt.core.dom.ast.IASTName;

public class MemoryStatus {
    private final VariableManager variableManager;
    private final MemoryManager memoryManager;

    public MemoryStatus() {
        this.variableManager = new VariableManager();
        this.memoryManager = new MemoryManager();
    }

    public MemoryStatus(MemoryStatus o) {
        this.variableManager = new VariableManager(o.variableManager);
        this.memoryManager = new MemoryManager(o.memoryManager);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MemoryStatus)) {
            return false;
        }
        MemoryStatus s = (MemoryStatus) o;
        return this.variableManager.equals(s.variableManager)
            && this.memoryManager.equals(s.memoryManager);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.variableManager.hashCode();
        result = 31 * result + this.memoryManager.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MemoryStatus(" + variableManager.toString() + "," + memoryManager.toString() + ")";
    }

    public void update(Variable... vs) {
        for (Variable v : vs) {
            this.variableManager.put(v);
        }
    }

    public VariableManager variableManager() {
        return this.variableManager;
    }

    public MemoryManager memoryManager() {
        return this.memoryManager;
    }
}
