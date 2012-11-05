package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;

import org.eclipse.jface.text.BadLocationException;

import com.github.uchan_nos.c_helper.analysis.CFG;

import com.github.uchan_nos.c_helper.dataflow.EntryExitPair;

import com.github.uchan_nos.c_helper.pointer.MemoryBlock;
import com.github.uchan_nos.c_helper.pointer.MemoryStatus;
import com.github.uchan_nos.c_helper.pointer.PointToSolver;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * メモリリーク，2重freeなどを指摘する.
 * @author uchan
 *
 */
public class MemoryLeakSuggester extends Suggester {

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        try {
            for (Map.Entry<String, CFG> entry : input.getProcToCFG().entrySet()) {
                CFG cfg = entry.getValue();

                PointToSolver solver =
                        new PointToSolver(cfg, cfg.entryVertex());
                PointToSolver.Result<CFG.Vertex, MemoryStatus> result = solver.solve();
                Set<PointToSolver.Problem> problems = solver.problems();

                for (CFG.Vertex v : Util.sort(result.analysisValue.keySet())) {
                    EntryExitPair<MemoryStatus> memoryStatuses = result.analysisValue.get(v);
                    System.out.println(v.label() + ": exit");

                    // 関数から抜ける頂点かどうか
                    boolean leavingNode = v.equals(cfg.exitVertex())
                        || (v.getASTNode() != null && v.getASTNode() instanceof IASTReturnStatement);

                    int memoryLeakFound = 0;
                    for (MemoryStatus memoryStatus : memoryStatuses.exit()) {
                        System.out.println("  " + memoryStatus);

                        for (MemoryBlock b : memoryStatus.memoryManager().memoryBlocks()) {
                            if (b.allocated() && (leavingNode || b.refCount() == 0)) {
                                memoryLeakFound++;
                                break;
                                //System.out.println("    メモリリーク検出: " + b);
                            }
                        }
                    }

                    if (memoryLeakFound > 0) {
                        suggestions.add(new Suggestion(
                                    input.getSource(),
                                    v.getASTNode(),
                                    memoryLeakFound == memoryStatuses.exit().size() ?
                                    "メモリリークが発生します" :
                                    "メモリリークが発生する可能性があります",
                                    null));
                    }

                    for (PointToSolver.Problem p : problems) {
                        if (v.getASTNode().contains(p.position)) {
                            String message = null;
                            if ("未初期化変数をfreeしてはいけない".equals(p.message)) {
                                message = memoryStatuses.exit().size() == 1 ?
                                    "未初期化変数をfreeしてはいけません" :
                                    "未初期化変数をfreeする可能性があります";
                            } else if ("既に解放されている領域をfreeしてはいけない".equals(p.message)) {
                                message = memoryStatuses.exit().size() == 1 ?
                                    "既に解放されている領域を2重にfreeしてはいけません" :
                                    "既に開放されている領域を2重にfreeする可能性があります";
                            } else {
                                message = p.message;
                            }

                            suggestions.add(new Suggestion(
                                        input.getSource(),
                                        v.getASTNode(),
                                        message,
                                        null));
                            //System.out.println("    " + p.message);
                        }
                    }

                    //System.out.println();
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return suggestions;
    }
}
