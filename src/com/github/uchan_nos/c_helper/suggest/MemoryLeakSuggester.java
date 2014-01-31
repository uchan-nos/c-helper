package com.github.uchan_nos.c_helper.suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;

import org.eclipse.jface.text.BadLocationException;
import org.osgi.service.log.LogEntry;

import com.github.uchan_nos.c_helper.Activator;
import com.github.uchan_nos.c_helper.analysis.CFG;

import com.github.uchan_nos.c_helper.dataflow.EntryExitPair;

import com.github.uchan_nos.c_helper.pointer.MemoryBlock;
import com.github.uchan_nos.c_helper.pointer.MemoryProblem;
import com.github.uchan_nos.c_helper.pointer.MemoryStatus;
import com.github.uchan_nos.c_helper.pointer.PointToSolver;

import com.github.uchan_nos.c_helper.resource.StringResource;

import com.github.uchan_nos.c_helper.util.Util;

/**
 * メモリリーク，2重freeなどを指摘する.
 * @author uchan
 *
 */
public class MemoryLeakSuggester extends Suggester {

    private Logger logger = Activator.getLogger();

    @Override
    public Collection<Suggestion> suggest(SuggesterInput input, AssumptionManager assumptionManager) {
        ArrayList<Suggestion> suggestions = new ArrayList<Suggestion>();

        try {
            for (Map.Entry<String, CFG> entry : input.getProcToCFG().entrySet()) {
                CFG cfg = entry.getValue();

                PointToSolver solver =
                        new PointToSolver(cfg, cfg.entryVertex());
                PointToSolver.Result<CFG.Vertex, MemoryStatus> result = solver.solve();
                Set<MemoryProblem> problems = solver.problems();

                StringBuilder resultMessage = new StringBuilder();

                for (CFG.Vertex v : Util.sort(result.analysisValue.keySet())) {
                    EntryExitPair<MemoryStatus> memoryStatuses = result.analysisValue.get(v);
                    resultMessage.append(v.label() + ": exit\n");

                    // 関数から抜ける頂点かどうか
                    boolean leavingNode = v.equals(cfg.exitVertex())
                        || (v.getASTNode() != null && v.getASTNode() instanceof IASTReturnStatement);

                    int memoryLeakFound = 0;
                    for (MemoryStatus memoryStatus : memoryStatuses.exit()) {
                        resultMessage.append("  " + memoryStatus + "\n");

                        for (MemoryBlock b : memoryStatus.memoryManager().memoryBlocks()) {
                            if (b.allocated() && (leavingNode || b.refCount() == 0)) {
                                memoryLeakFound++;
                                break;
                                //System.out.println("    メモリリーク検出: " + b);
                            }
                        }
                    }

                    if (memoryLeakFound > 0) {
                        Suggestion s;
                        String message = memoryLeakFound == memoryStatuses.exit().size() ?
                            "メモリリークする" :
                            "メモリリークする可能性がある";
                        message = StringResource.get(message);

                        if (v.getASTNode() == null) {
                            int line = -1, column = -1, offset = -1, length = -1;
                            if (v.equals(cfg.exitVertex())) {
                                // 関数定義の一番最後の場所を取得
                                IASTNode node = cfg.entryVertex().getASTNode().getParent();
                                if (node instanceof IASTCompoundStatement) {
                                    offset = node.getFileLocation().getNodeOffset()
                                        + node.getFileLocation().getNodeLength() - 1;
                                    length = 1;
                                    line = node.getFileLocation().getEndingLineNumber() - 1;
                                    column = Util.calculateColumnNumber(input.getSource(), offset);
                                }
                            }
                            s = new Suggestion(input.getFilePath(), line, column, offset, length, message, null);
                        } else {
                            s = new Suggestion(input.getSource(), v.getASTNode(), message, null);
                        }

                        suggestions.add(s);
                    }

                    for (MemoryProblem p : problems) {
                        if (v.getASTNode().contains(p.position)) {
                            boolean unconditionallyHappen = memoryStatuses.exit().size() == 1;

                            String message = null;
                            if (p.message != null) {
                                switch (p.message) {
                                case DOUBLE_FREE:
                                    message = unconditionallyHappen
                                        ? "同じ領域を2重にfreeしてはいけない"
                                        : "同じ領域を2重にfreeしてしまう可能性がある";
                                    break;
                                case UNINITIALIZED_VALUE_FREE:
                                    message = unconditionallyHappen
                                        ? "未初期化変数をfreeしてはいけない"
                                        : "未初期化変数をfreeしてしまう可能性がある";
                                    break;
                                case UNKNOWN_VALUE_FREE:
                                    message = unconditionallyHappen
                                        ? "mallocやcalloc、reallocで確保した領域以外をfreeしてはいけない"
                                        : "mallocやcalloc、reallocで確保した領域以外をfreeしてしまう可能性がある";
                                    break;
                                default:
                                    message = "Unknown";
                                }
                            }

                            suggestions.add(new Suggestion(
                                        input.getSource(),
                                        v.getASTNode(),
                                        StringResource.get(message),
                                        null));
                            //System.out.println("    " + p.message);
                        }
                    }

                    //System.out.println();
                }

                logger.fine(resultMessage.toString());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        return suggestions;
    }
}
