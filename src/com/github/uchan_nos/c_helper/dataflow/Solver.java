package com.github.uchan_nos.c_helper.dataflow;

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

    /**
     * 解析の結果.
     * 内部的には解析の途中経過の記憶にも使う.
     */
    public static class Result<Vertex, Value> {
        public final Map<Vertex, EntryExitPair<Value>> analysisValue;

        public Result(Map<Vertex, EntryExitPair<Value>> analysisValue) {
            this.analysisValue = analysisValue;
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
     * 解析の初期情報を返す.
     * 前進解析ではフローグラフの入口ノードの入口値、
     * 後進解析ではフローグラフの出口ノードの出口値となる.
     */
    protected abstract Set<Value> getInitValue();

    /**
     * 解析値を格納する集合の初期値を新しく作って返す.
     * 前進解析における入口ノードの入口値や
     * 後進解析における出口ノードの出口値以外の集合の初期値.
     */
    protected abstract Set<Value> createDefaultSet();

    /**
     * 指定された頂点における遷移関数.
     * 前進解析では入口値を基に出口値を計算する.
     * @return result が変更されたら true
     */
    protected abstract boolean transfer(Vertex v, Set<Value> entry, Set<Value> result);

    /**
     * join演算子.
     * result に set を加える.
     * @return result が変更されたら true
     */
    protected abstract boolean join(Set<Value> result, Set<Value> set);

    /**
     * 集合のコピー.
     */
    protected abstract Set<Value> clone(Set<Value> set);
}
