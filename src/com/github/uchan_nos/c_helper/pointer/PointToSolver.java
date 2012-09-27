package com.github.uchan_nos.c_helper.pointer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.CFG.Vertex;
import com.github.uchan_nos.c_helper.analysis.CFGCreator;
import com.github.uchan_nos.c_helper.analysis.IGraph;
import com.github.uchan_nos.c_helper.analysis.Parser;

import com.github.uchan_nos.c_helper.dataflow.ForwardSolver;

import com.github.uchan_nos.c_helper.util.Util;

public class PointToSolver extends ForwardSolver<CFG.Vertex, MemoryStatus> {
    public PointToSolver(IGraph<CFG.Vertex> cfg, CFG.Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    @Override
    protected Set<MemoryStatus> getInitValue() {
        // すべての変数が UNDEFINED であるメモリ状態だけを含む集合が初期値となる
        Set<MemoryStatus> result = new HashSet<MemoryStatus>();
        result.add(new MemoryStatus());
        return result;
    }

    @Override
    protected Set<MemoryStatus> createDefaultSet() {
        return new HashSet<MemoryStatus>();
    }

    @Override
    protected boolean transfer(Vertex v, Set<MemoryStatus> entry,
            Set<MemoryStatus> result) {
        return result.addAll(analyze(v.getASTNode(), entry));
    }

    @Override
    protected boolean join(Set<MemoryStatus> result, Set<MemoryStatus> set) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Set<MemoryStatus> clone(Set<MemoryStatus> set) {
        // TODO Auto-generated method stub
        return null;
    }

    private boolean transfer(IASTNode ast, Set<MemoryStatus> entry,
            Set<MemoryStatus> result) {
        return false;
    }

    public Set<MemoryStatus> analyze(IASTNode ast, Set<MemoryStatus> entry) {
        // malloc呼び出しへのすべてのパスを取得
        List<List<IASTNode>> pathToMalloc =
            MallocCallFinder.findPathToMalloc(ast);

        if (pathToMalloc.size() == 1) {
            return analyzeMalloc(pathToMalloc.get(0), entry);
        } else if (pathToMalloc.size() > 1) {
            throw new UnsupportedOperationException();
        } else {
            return entry;
        }
    }

    private Set<MemoryStatus> analyzeMalloc(
            List<IASTNode> pathToMalloc, Set<MemoryStatus> entry) {

        // malloc呼び出しのノードから上方向へトラバース
        ListIterator<IASTNode> it = pathToMalloc.listIterator(pathToMalloc.size());
        IASTNode node = it.previous();

        // pathToMallocの一番後ろの要素はmalloc呼び出し式でなければならない
        assert node instanceof IASTFunctionCallExpression
            && Util.getName(((IASTFunctionCallExpression) node).getFunctionNameExpression())
                .resolveBinding().equals("malloc");

        // malloc呼び出し後の状態を計算
        Set<MallocEvalElement> afterMallocStatusSet = evalMalloc(entry);

        // a = b = .. = malloc(..)
        // という形なら=の続く限り解析し、最終的な状態を全体の状態とする
        // 他の形の場合、取り敢えずmalloc呼び出し直後の状態を全体の状態とする
        if (it.hasPrevious()) {
            node = it.previous();

            if (Util.isIASTBinaryExpression(node, IASTBinaryExpression.op_assign)) {
                // pathToMallocの後ろから二番目の要素が代入式である場合
                IASTBinaryExpression be = (IASTBinaryExpression) node;
                IASTName lhsName = Util.getName(be.getOperand1());
                IBinding lhsBinding = lhsName == null ? null : lhsName.resolveBinding();

                if (lhsBinding instanceof IVariable) {
                    // hoge = malloc()
                    Set<MemoryStatus> intermediateStatus = evalAssignMallocToVariable(be, afterMallocStatusSet);

                    IVariable rhsBinding = (IVariable) lhsBinding;
                    while (it.hasPrevious() && rhsBinding != null) {
                        node = it.previous();
                        if (Util.isIASTBinaryExpression(node, IASTBinaryExpression.op_assign)) {
                            be = (IASTBinaryExpression) node;
                            lhsName = Util.getName(be.getOperand1());
                            lhsBinding = lhsName == null ? null : lhsName.resolveBinding();

                            // hoge = foo
                            intermediateStatus = evalAssignVariableToVariable(
                                    be, rhsBinding, intermediateStatus);

                            rhsBinding = lhsBinding instanceof IVariable ? (IVariable) lhsBinding : null;
                        } else {
                            break;
                        }
                    }

                    if (node == pathToMalloc.get(0)) {
                        // 最後までトラバースできた場合
                        return intermediateStatus;
                    } else {
                        System.out.println("Not supported syntax");
                        return intermediateStatus;
                    }
                }

            }
        }

        Set<MemoryStatus> result = new HashSet<MemoryStatus>();
        for (MallocEvalElement elem : afterMallocStatusSet) {
            result.add(elem.afterStatus);
        }
        return result;
    }

    private static class MallocEvalElement {
        public final MemoryStatus afterStatus; // malloc呼び出しにより変化したメモリ状態
        public final Address allocatedAddress; // 生成されたヒープ領域へのアドレスまはたNULLアドレス
        public MallocEvalElement(MemoryStatus afterStatus, Address allocatedAddress) {
            this.afterStatus = afterStatus;
            this.allocatedAddress = allocatedAddress;
        }
    }

    // malloc(..)
    private Set<MallocEvalElement> evalMalloc(Set<MemoryStatus> entry) {
        Set<MallocEvalElement> result = new HashSet<MallocEvalElement>();

        for (MemoryStatus s : entry) {
            // mallocがメモリ生成に失敗した場合
            result.add(new MallocEvalElement(s, InvalidAddress.NULL));

            // mallocがメモリ生成した場合
            MemoryStatus newStatus = new MemoryStatus(s);
            MemoryBlock b = newStatus.memoryManager().allocate();
            result.add(new MallocEvalElement(newStatus, new HeapAddress(b.id())));
        }

        return result;
    }

    // p = malloc(..)
    private Set<MemoryStatus> evalAssignMallocToVariable(
            IASTBinaryExpression assignNode, Set<MallocEvalElement> entry) {
        Set<MemoryStatus> result = new HashSet<MemoryStatus>();

        if (assignNode.getOperand1() instanceof IASTIdExpression) {
            IBinding lhsBinding = Util.getName(assignNode.getOperand1()).resolveBinding();

            if (lhsBinding instanceof IVariable) {
                // 普通の変数への代入
                for (MallocEvalElement elem : entry) {
                    MemoryStatus newStatus = new MemoryStatus(elem.afterStatus);
                    if (elem.allocatedAddress == InvalidAddress.NULL) {
                        newStatus.variableManager().put(new Variable(
                                    (IVariable) lhsBinding,
                                    Variable.States.NULL,
                                    null));
                    } else {
                        if (elem.allocatedAddress instanceof HeapAddress) {
                            MemoryBlock b = newStatus.memoryManager().find(
                                    ((HeapAddress) elem.allocatedAddress).memoryBlockId());
                            b.ref();
                        }
                        newStatus.variableManager().put(new Variable(
                                    (IVariable) lhsBinding,
                                    Variable.States.POINTING,
                                    elem.allocatedAddress));
                    }

                    result.add(newStatus);
                }
            }
        }
        return result;
    }

    // p = q
    private Set<MemoryStatus> evalAssignVariableToVariable(
            IASTBinaryExpression assignNode, IVariable rhs, Set<MemoryStatus> entry) {
        Set<MemoryStatus> result = new HashSet<MemoryStatus>();

        if (assignNode.getOperand1() instanceof IASTIdExpression) {
            IBinding lhsBinding = Util.getName(assignNode.getOperand1()).resolveBinding();

            if (lhsBinding instanceof IVariable) {
                // 普通の変数への代入
                IVariable lhs = (IVariable) lhsBinding;

                for (MemoryStatus status : entry) {
                    MemoryStatus newStatus = new MemoryStatus(status);
                    VariableManager vm = newStatus.variableManager();

                    if (vm.getVariableStatus(lhs) == Variable.States.POINTING) {
                        Address value = vm.get(lhs).value();
                        if (value instanceof HeapAddress) {
                            MemoryBlock b = newStatus.memoryManager().find(
                                    ((HeapAddress) value).memoryBlockId());
                            b.unref();
                        }
                    }

                    if (vm.getVariableStatus(rhs) == Variable.States.POINTING) {
                        Address value = vm.get(rhs).value();
                        if (value instanceof HeapAddress) {
                            MemoryBlock b = newStatus.memoryManager().find(
                                    ((HeapAddress) value).memoryBlockId());
                            b.ref();
                        }
                    }

                    newStatus.variableManager().put(new Variable(
                                (IVariable) lhsBinding,
                                vm.get(rhs).status(),
                                vm.get(rhs).value()));

                    result.add(newStatus);
                }
            }
        }

        return result;
    }

    public static void main(String[] args) {
        String fileContent =
            "#include <stdlib.h>\n" +
            "void f(void) {\n" +
            "  char *p = malloc(10), *q;\n" +
            "  q = NULL;\n" +
            "  q = malloc(20);\n" +
            "  p = q = malloc(30);\n" +
            "}\n";

        IASTTranslationUnit translationUnit =
                new Parser("", fileContent).parseOrNull();
        Map<String, CFG> procToCFG =
                new CFGCreator(translationUnit).create();

        PointToSolver solver = new PointToSolver(null, null);
        Set<MemoryStatus> initialStatus = new HashSet<MemoryStatus>();
        initialStatus.add(new MemoryStatus());

        for (Entry<String, CFG> entry : procToCFG.entrySet()) {
            System.out.println(entry.getKey());

            Set<CFG.Vertex> visited = new HashSet<CFG.Vertex>();
            ArrayDeque<CFG.Vertex> visiting = new ArrayDeque<CFG.Vertex>();

            CFG cfg = entry.getValue();
            visiting.add(cfg.entryVertex());

            CFG.Vertex v = null;
            while ((v = visiting.poll()) != null) {
                if (!visited.contains(v)) {
                    visited.add(v);
                    for (CFG.Vertex next : cfg.getConnectedVerticesFrom(v)) {
                        visiting.add(next);
                    }

                    Set<MemoryStatus> afterStatus = solver.analyze(
                            v.getASTNode(), initialStatus);
                    System.out.println(afterStatus.toString());
                }
            }
        }
    }
}
