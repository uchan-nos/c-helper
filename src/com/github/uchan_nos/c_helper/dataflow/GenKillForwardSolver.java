package com.github.uchan_nos.c_helper.dataflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.uchan_nos.c_helper.analysis.IGraph;

public abstract class GenKillForwardSolver<Vertex, Value> extends ForwardSolver<Vertex, Value> {
    public GenKillForwardSolver(IGraph<Vertex> cfg, Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    private Map<Vertex, GenKill<Value>> genkill = null;

    /**
     * データフロー解析を行い、結果を返す.
     */
    @Override
    public Result<Vertex, Value> solve() {
        // gen/kill集合を生成
        this.genkill = new HashMap<Vertex, GenKill<Value>>();
        for (Vertex v : getCFG().getVertices()) {
            genkill.put(v, getGenKill(v));
        }

        // 解析実行
        Result<Vertex, Value> result = super.solve();

        // 後片付け
        this.genkill = null;
        return result;
    }

    /**
     * 遷移関数.
     */
    protected boolean transfer(Vertex v, Set<Value> entry, Set<Value> result) {
        Set<Value> oldResult = clone(result);

        final GenKill<Value> gk = genkill.get(v);
        result.addAll(entry);
        result.removeAll(gk.kill);
        result.addAll(gk.gen);

        return !result.equals(oldResult);
    }

    /**
     * 指定した頂点に対応するgen/kill集合を得る.
     */
    protected abstract GenKill<Value> getGenKill(Vertex v);
}
