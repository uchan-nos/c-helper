package com.github.uchan_nos.c_helper.dataflow;

import java.util.Set;

/**
 * 各頂点における入口値と出口値を表す.
 */
public class EntryExitPair<Value> {
    private Set<Value> entry;
    private Set<Value> exit;

    public EntryExitPair(Set<Value> entry, Set<Value> exit) {
        this.entry = entry;
        this.exit = exit;
    }

    public Set<Value> entry() {
        return this.entry;
    }

    public Set<Value> exit() {
        return this.exit;
    }

    public void setEntry(Set<Value> entry) {
        this.entry = entry;
    }

    public void setExit(Set<Value> exit) {
        this.exit = exit;
    }
}
