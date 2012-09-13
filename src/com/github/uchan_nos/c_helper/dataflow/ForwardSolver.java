package com.github.uchan_nos.c_helper.dataflow;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTIdExpression;

import com.github.uchan_nos.c_helper.analysis.DummyAssignExpression;
import com.github.uchan_nos.c_helper.analysis.IGraph;

public abstract class ForwardSolver<Vertex, Value> extends Solver<Vertex, Value> {
    public ForwardSolver(IGraph<Vertex> cfg, Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    /**
     * データフロー解析を行い、結果を返す.
     */
    public Result<Vertex, Value> solve() {
        Result<Vertex, Value> sets = new Result<Vertex, Value>(
                new HashMap<Vertex, Set<Value>>(),
                new HashMap<Vertex, Set<Value>>());

        // 集合を初期化する
        for (Vertex v : getCFG().getVertices()) {
            sets.entrySet.put(v, getInitValue(v));
        }

        // forward 解析する
        solveForward(sets);
        return sets;
    }

    private void solveForward(Result<Vertex, Value> sets) {
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

                // 頂点 v へ接続されている頂点の出口値を取得
                Set<Set<Value>> exitSets = new HashSet<Set<Value>>();
                for (Vertex prevVertex : getCFG().getConnectedVerticesTo(v)) {
                    Set<Value> exitSet = sets.exitSet.get(prevVertex);
                    if (exitSet != null) {
                        exitSets.add(exitSet);
                    }
                }

                // すべての出口値を join して頂点 v の入口値とする
                Set<Value> entrySet;
                if (v.equals(getEntryVertex())) {
                    entrySet = entryOfEntryVertex;
                } else {
                    entrySet = join(exitSets);
                    sets.entrySet.put(v, entrySet);
                }

                // 頂点 v の入口値を基に、遷移関数で 出口値 を計算する
                Set<Value> exitSet = transfer(v, entrySet);
                sets.exitSet.put(v, exitSet);
            }
        } while (!sets.exitSet.equals(prevExitSet));
    }
}
