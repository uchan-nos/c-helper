package com.github.uchan_nos.c_helper.dataflow;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.github.uchan_nos.c_helper.analysis.IGraph;

public abstract class GenKillForwardSolver<Vertex, Value> extends ForwardSolver<Vertex, Value> {
    public GenKillForwardSolver(IGraph<Vertex> cfg, Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    /**
     * データフロー解析を行い、結果を返す.
     */
    @Override
    public Result<Vertex,Value> solve() {
        Result<Vertex, Value> sets = new Result<Vertex, Value>(
                new HashMap<Vertex, Set<Value>>(),
                new HashMap<Vertex, Set<Value>>());

        Map<Vertex, GenKill<Value>> genkill = new HashMap<Vertex, GenKill<Value>>();

        // 集合を初期化する
        for (Vertex v : getCFG().getVertices()) {
            sets.entrySet.put(v, getInitValue(v));
            genkill.put(v, getGenKill(v));
        }

        // forward 解析する
        solveForward(sets, genkill);
        return sets;
    }

    private void solveForward(Result<Vertex, Value> sets, Map<Vertex, GenKill<Value>> genkill) {
        Queue<Vertex> remainVertices = new ArrayDeque<Vertex>();
        Set<Vertex> visitedVertices = new HashSet<Vertex>();

        Map<Vertex, Set<Value>> prevExitSet = new HashMap<Vertex, Set<Value>>();
        final Set<Value> entryOfEntryVertex = sets.entrySet.get(getEntryVertex());

        Vertex v;
        do {
            prevExitSet.putAll(sets.exitSet);
            visitedVertices.clear();
            remainVertices.add(getEntryVertex());

            while ((v = remainVertices.poll()) != null && !visitedVertices.contains(v)) {
                visitedVertices.add(v);
                Set<Vertex> connectedVertices = getCFG().getConnectedVerticesFrom(v);
                for (Vertex nextVisit : connectedVertices) {
                    remainVertices.add(nextVisit);
                }

                // 頂点 v の入口値を取得
                Set<Value> entrySet = getEntrySet(v, entryOfEntryVertex, sets);
                sets.entrySet.put(v, entrySet);

                // 頂点 v の入口値を基に、gen/killで 出口値 を計算する
                Set<Value> exitSet = new HashSet<Value>(entrySet);
                exitSet.removeAll(genkill.get(v).kill);
                exitSet.addAll(genkill.get(v).gen);
                sets.exitSet.put(v, exitSet);
            }
        } while (!sets.exitSet.equals(prevExitSet));
    }

    /**
     * 遷移関数.
     * gen/kill形式では使わないので封印.
     */
    protected final Set<Value> transfer(Vertex v, Set<Value> set) {
        return null;
    }

    /**
     * 指定した頂点に対応するgen/kill集合を得る.
     */
    protected abstract GenKill<Value> getGenKill(Vertex v);
}
