package com.github.uchan_nos.c_helper.dataflow;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.github.uchan_nos.c_helper.analysis.IGraph;

/**
 * データフロー解析を行うクラス.
 * @author uchan
 */
public abstract class Solver<Vertex, Value> {
    private final IGraph<Vertex> cfg;
    private final Vertex entryVertex;

    public static class Result<Vertex, Value> {
        public final Map<Vertex, Set<Value>> entrySet;
        public final Map<Vertex, Set<Value>> exitSet;

        public Result(Map<Vertex, Set<Value>> entrySet, Map<Vertex, Set<Value>> exitSet) {
            this.entrySet = entrySet;
            this.exitSet = exitSet;
        }
    }

    public Solver(IGraph<Vertex> cfg, Vertex entryVertex) {
        this.cfg = cfg;
        this.entryVertex = entryVertex;
    }

    protected IGraph<Vertex> getCFG() {
        return cfg;
    }

    protected Vertex getEntryVertex() {
        return entryVertex;
    }

    /**
     * データフロー解析を行い、結果を返す.
     */
    public abstract Result<Vertex, Value> solve();

    /**
     * 初期値を返す.
     */
    public abstract Set<Value> getInitValue(Vertex v);

    /**
     * 遷移関数.
     */
    public abstract Set<Value> transfer(Vertex v, Set<Value> set);

    /**
     * join演算子.
     */
    public abstract Set<Value> join(Collection<Set<Value>> sets);
}
