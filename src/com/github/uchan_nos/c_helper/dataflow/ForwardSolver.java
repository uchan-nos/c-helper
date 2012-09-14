package com.github.uchan_nos.c_helper.dataflow;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


import com.github.uchan_nos.c_helper.analysis.IGraph;

public abstract class ForwardSolver<Vertex, Value> extends Solver<Vertex, Value> {
    public ForwardSolver(IGraph<Vertex> cfg, Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    /**
     * データフロー解析を行い、結果を返す.
     */
    @Override
    public Result<Vertex, Value> solve() {
        Map<Vertex, EntryExitPair<Value>> analysisValue =
            new HashMap<Vertex, EntryExitPair<Value>>();

        // 集合を初期化する
        for (Vertex v : getCFG().getVertices()) {
            analysisValue.put(v, new EntryExitPair<Value>(
                        v.equals(getEntryVertex()) ? getInitValue() : createDefaultSet(), // entry
                        createDefaultSet() // exit
                        ));
        }

        // forward 解析する
        solveForward(analysisValue);
        return new Result<Vertex, Value>(analysisValue);
    }

    private void solveForward(Map<Vertex, EntryExitPair<Value>> analysisValue) {
        Queue<Vertex> remainVertices = new ArrayDeque<Vertex>();
        Set<Vertex> visitedVertices = new HashSet<Vertex>();

        Vertex v;
        boolean modified;
        do {
            modified = false;
            visitedVertices.clear();
            remainVertices.add(getEntryVertex());

            while ((v = remainVertices.poll()) != null && !visitedVertices.contains(v)) {
                visitedVertices.add(v);
                Set<Vertex> connectedVertices = getCFG().getConnectedVerticesFrom(v);
                for (Vertex nextVisit : connectedVertices) {
                    remainVertices.add(nextVisit);
                }

                // 頂点 v の解析値を取得
                final EntryExitPair<Value> vInfo = analysisValue.get(v);

                // 頂点 v の入口値の計算
                // 頂点 v に接続している各頂点の出口値をjoinする
                for (Vertex prevVertex : getCFG().getConnectedVerticesTo(v)) {
                    final Set<Value> exitSet = analysisValue.get(prevVertex).exit();
                    modified |= join(vInfo.entry(), exitSet);
                }

                // 頂点 v の出口値の計算
                // 頂点 v の入口値を基に、遷移関数で出口値を計算する
                modified |= transfer(v, vInfo.entry(), vInfo.exit());
            }
        } while (modified);
    }
}
