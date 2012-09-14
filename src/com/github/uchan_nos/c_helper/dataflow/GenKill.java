package com.github.uchan_nos.c_helper.dataflow;

import java.util.Set;

public class GenKill<Value> {
    public final Set<Value> gen;
    public final Set<Value> kill;

    public GenKill(Set<Value> gen, Set<Value> kill) {
        this.gen = gen;
        this.kill = kill;
    }
}
