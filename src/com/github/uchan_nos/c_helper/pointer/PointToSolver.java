package com.github.uchan_nos.c_helper.pointer;

import java.io.File;
import java.io.IOException;

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

import org.eclipse.core.runtime.CoreException;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.CFG.Vertex;
import com.github.uchan_nos.c_helper.analysis.CFGCreator;
import com.github.uchan_nos.c_helper.analysis.IGraph;
import com.github.uchan_nos.c_helper.analysis.Parser;

import com.github.uchan_nos.c_helper.dataflow.EntryExitPair;
import com.github.uchan_nos.c_helper.dataflow.ForwardSolver;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class PointToSolver extends ForwardSolver<CFG.Vertex, MemoryStatus> {
    /**
     * 解析の途中で検出した問題を表す.
     * 例えばfreeに未初期化変数を渡しているとか、freeにmallocした以外の値を渡しているなど
     */
    public static class Problem {
        public final IASTNode position;
        public final String message;
        public Problem(IASTNode position, String message) {
            this.position = position;
            this.message = message;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Problem)) {
                return false;
            }
            Problem p = (Problem) o;
            return Util.equalsOrBothNull(position, p.position)
                && Util.equalsOrBothNull(message, p.message);
        }

        @Override
        public final int hashCode() {
            int result = 17;
            result = 31 * result + (position == null ? 0 : position.hashCode());
            result = 31 * result + (message == null ? 0 : message.hashCode());
            return result;
        }
    }

    private Set<Problem> problems = new HashSet<Problem>();

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
        return result.addAll(set);
    }

    @Override
    protected Set<MemoryStatus> clone(Set<MemoryStatus> set) {
        return new HashSet<MemoryStatus>(set);
    }

    /**
     * 解析の途中で検出した問題を返す.
     * 例えばfreeに未初期化変数を渡しているとか、freeにmallocした以外の値を渡しているなど
     */
    public Set<Problem> problems() {
        return this.problems;
    }

    private Set<MemoryStatus> analyze(IASTNode ast, Set<MemoryStatus> entry) {
        if (!(ast instanceof IASTExpressionStatement)) {
            return entry;
        }

        // ヒープメモリブロックの数が変数の数を上回っていたら解析中止
        for (MemoryStatus s : entry) {
            int numVariables = s.variableManager().getContainingVariables().size();
            //if (numVariables > 0 && numVariables < s.memoryManager().memoryBlocks().size()) {
            int numAllocatedBlocks = 0;
            for (MemoryBlock b : s.memoryManager().memoryBlocks()) {
                if (b.allocated()) {
                    numAllocatedBlocks++;
                }
            }
            if (numVariables > 0 && numVariables < numAllocatedBlocks) {
                return entry;
            }
        }

        // malloc呼び出しへのすべてのパスを取得
        ASTFilter.Predicate mallocCallPredicate = ASTPathFinder.createFunctionCallPredicate("malloc");
        List<List<IASTNode>> pathToMalloc = ASTPathFinder.findPath(ast, mallocCallPredicate);

        // free呼び出しへのすべてのパスを取得
        ASTFilter.Predicate freeCallPredicate = ASTPathFinder.createFunctionCallPredicate("free");
        List<List<IASTNode>> pathToFree = ASTPathFinder.findPath(ast, freeCallPredicate);

        // realloc呼び出しへのすべてのパスを取得
        ASTFilter.Predicate reallocCallPredicate = ASTPathFinder.createFunctionCallPredicate("realloc");
        List<List<IASTNode>> pathToRealloc = ASTPathFinder.findPath(ast, reallocCallPredicate);

        // 変数代入へのすべてのパスを取得
        ASTFilter.Predicate variableAssignPredicate = new ASTFilter.Predicate() {
            @Override public boolean pass(IASTNode node) {
                if (Util.isIASTBinaryExpression(node, IASTBinaryExpression.op_assign)) {
                    IASTBinaryExpression be = (IASTBinaryExpression) node;
                    return be.getOperand2() instanceof IASTIdExpression ||
                        be.getOperand2().getRawSignature().equals("NULL");
                }
                return false;
            }
        };
        List<List<IASTNode>> pathToVariableAssign = ASTPathFinder.findPath(ast, variableAssignPredicate);

        if (pathToMalloc.size() >= 1) {
            if (pathToMalloc.size() == 1) {
                return analyzeMalloc(pathToMalloc.get(0), entry);
            }
            throw new UnsupportedOperationException();
        } else if (pathToFree.size() >= 1) {
            if (pathToFree.size() == 1) {
                return analyzeFree(pathToFree.get(0), entry);
            }
            throw new UnsupportedOperationException();
        } else if (pathToRealloc.size() >= 1) {
            if (pathToRealloc.size() == 1) {
                return analyzeRealloc(pathToRealloc.get(0), entry);
            }
            throw new UnsupportedOperationException();
        } else if (pathToVariableAssign.size() >= 1) {
            if (pathToVariableAssign.size() == 1) {
                return analyzeVariableAssign(pathToVariableAssign.get(0), entry);
            }
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
                .resolveBinding().getName().equals("malloc");

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
                IVariable lhs = (IVariable) lhsBinding;

                for (MallocEvalElement elem : entry) {
                    MemoryStatus newStatus = new MemoryStatus(elem.afterStatus);

                    unrefIfPointingToHeapAddress(lhs, newStatus);

                    if (elem.allocatedAddress == InvalidAddress.NULL) {
                        newStatus.variableManager().put(new Variable(
                                    lhs,
                                    Variable.States.NULL,
                                    null));
                    } else {
                        if (elem.allocatedAddress instanceof HeapAddress) {
                            MemoryBlock b = newStatus.memoryManager().find(
                                    ((HeapAddress) elem.allocatedAddress).memoryBlockId());
                            b.ref();
                        }
                        newStatus.variableManager().put(new Variable(
                                    lhs,
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

                    unrefIfPointingToHeapAddress(lhs, newStatus);
                    refIfPointingToHeapAddress(rhs, newStatus);

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

    // p = NULL
    private Set<MemoryStatus> evalAssignNullToVariable(
            IASTBinaryExpression assignNode, Set<MemoryStatus> entry) {
        Set<MemoryStatus> result = new HashSet<MemoryStatus>();

        if (assignNode.getOperand1() instanceof IASTIdExpression) {
            IBinding lhsBinding = Util.getName(assignNode.getOperand1()).resolveBinding();

            if (lhsBinding instanceof IVariable) {
                // 普通の変数への代入
                IVariable lhs = (IVariable) lhsBinding;

                for (MemoryStatus elem : entry) {
                    MemoryStatus newStatus = new MemoryStatus(elem);

                    unrefIfPointingToHeapAddress(lhs, newStatus);

                    newStatus.variableManager().put(new Variable(
                                lhs,
                                Variable.States.NULL,
                                null));

                    result.add(newStatus);
                }
            }
        }
        return result;
    }


    private Set<MemoryStatus> analyzeFree(
            List<IASTNode> pathToFree, Set<MemoryStatus> entry) {

        // free呼び出しのノードから上方向へトラバース
        ListIterator<IASTNode> it = pathToFree.listIterator(pathToFree.size());
        IASTNode node = it.previous();

        // pathToFreeの一番後ろの要素はfree呼び出し式でなければならない
        assert node instanceof IASTFunctionCallExpression
            && Util.getName(((IASTFunctionCallExpression) node).getFunctionNameExpression())
                .resolveBinding().getName().equals("free");

        // free呼び出し後の状態を計算
        IASTExpression[] arguments = Util.getArguments((IASTFunctionCallExpression) node);
        if (arguments.length != 1) {
            System.out.println("freeの引数の数がおかしい: " + node.getRawSignature());
            return entry;
        }
        IASTName arg0Name = Util.getName(arguments[0]);
        IBinding arg0Binding;
        if (arg0Name == null || !((arg0Binding = arg0Name.resolveBinding()) instanceof IVariable)) {
            System.out.println("freeの引数が変数名ではない");
            return entry;
        }
        Set<FreeEvalElement> afterFreeStatusSet = evalFree(entry, (IVariable) arg0Binding);

        Set<MemoryStatus> result = new HashSet<MemoryStatus>();
        for (FreeEvalElement elem : afterFreeStatusSet) {
            if (elem.problem != null) {
                this.problems.add(new Problem(node, elem.problem));
            }
            result.add(elem.afterStatus);
        }

        return result;
    }

    private static class FreeEvalElement {
        public final MemoryStatus afterStatus; // free呼び出しにより変化したメモリ状態
        public final String problem; // free呼び出しで発見された問題
        public FreeEvalElement(MemoryStatus afterStatus, String problem) {
            this.afterStatus = afterStatus;
            this.problem = problem;
        }
    }

    // free(..)
    private Set<FreeEvalElement> evalFree(Set<MemoryStatus> entry, IVariable arg) {
        Set<FreeEvalElement> result = new HashSet<FreeEvalElement>();

        for (MemoryStatus s : entry) {
            MemoryStatus newStatus = new MemoryStatus(s);
            String problem = null;

            switch (s.variableManager().getVariableStatus(arg)) {
            case POINTING: {
                Address value = newStatus.variableManager().get(arg).value();
                if (value instanceof HeapAddress) {
                    MemoryBlock b = newStatus.memoryManager().find(
                            ((HeapAddress) value).memoryBlockId());
                    newStatus.memoryManager().release(b);
                } else {
                    System.out.println("freeの引数がヒープメモリを指していない");
                    problem = "freeの引数はヒープメモリを指している必要がある";
                }
            } break;
            case NULL:
                // do nothing
                break;
            case UNDEFINED:
                System.out.println("未初期化変数の使用");
                problem = "未初期化変数をfreeしてはいけない";
                break;
            }

            result.add(new FreeEvalElement(newStatus, problem));
        }

        return result;
    }

    private Set<MemoryStatus> analyzeRealloc(
            List<IASTNode> pathToRealloc, Set<MemoryStatus> entry) {

        // malloc呼び出しのノードから上方向へトラバース
        ListIterator<IASTNode> it = pathToRealloc.listIterator(pathToRealloc.size());
        IASTNode node = it.previous();

        // pathToReallocの一番後ろの要素はrealloc呼び出し式でなければならない
        assert node instanceof IASTFunctionCallExpression
            && Util.getName(((IASTFunctionCallExpression) node).getFunctionNameExpression())
                .resolveBinding().getName().equals("realloc");

        // realloc呼び出し後の状態を計算
        IASTExpression[] arguments = Util.getArguments((IASTFunctionCallExpression) node);
        if (arguments.length != 2) {
            System.out.println("reallocの引数の数がおかしい: " + node.getRawSignature());
            return entry;
        }
        IASTName arg0Name = Util.getName(arguments[0]);
        String arg1Signature = arguments[1].getRawSignature();

        Set<MallocEvalElement> afterReallocStatusSet = null;
        if (Util.equals(arg0Name.getSimpleID(), "NULL")) {
            afterReallocStatusSet = evalMalloc(entry);
        } else {
            IBinding arg0Binding;
            if (arg0Name == null || !((arg0Binding = arg0Name.resolveBinding()) instanceof IVariable)) {
                System.out.println("reallocの第1引数が変数名ではない");
                return entry;
            }
            if (arg1Signature.equals("0")) {
            } else {
                afterReallocStatusSet = evalRealloc(entry, (IVariable) arg0Binding);
            }
        }

        // a = b = .. = realloc(..)
        // という形なら=の続く限り解析し、最終的な状態を全体の状態とする
        // 他の形の場合、取り敢えずrealloc呼び出し直後の状態を全体の状態とする
        if (it.hasPrevious()) {
            node = it.previous();

            if (Util.isIASTBinaryExpression(node, IASTBinaryExpression.op_assign)) {
                // pathToReallocの後ろから二番目の要素が代入式である場合
                IASTBinaryExpression be = (IASTBinaryExpression) node;
                IASTName lhsName = Util.getName(be.getOperand1());
                IBinding lhsBinding = lhsName == null ? null : lhsName.resolveBinding();

                if (lhsBinding instanceof IVariable) {
                    // hoge = realloc()
                    Set<MemoryStatus> intermediateStatus = evalAssignMallocToVariable(be, afterReallocStatusSet);

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

                    if (node == pathToRealloc.get(0)) {
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
        for (MallocEvalElement elem : afterReallocStatusSet) {
            result.add(elem.afterStatus);
        }
        return result;
    }

    // realloc(..)
    // ptr, sizeともに0ではない場合の処理
    private Set<MallocEvalElement> evalRealloc(Set<MemoryStatus> entry, IVariable arg) {
        Set<MallocEvalElement> result = new HashSet<MallocEvalElement>();

        for (MemoryStatus s : entry) {
            // reallocがサイズ変更に失敗した場合
            result.add(new MallocEvalElement(s, InvalidAddress.NULL));

            // reallocがサイズ変更に成功した場合
            MemoryStatus newStatus = new MemoryStatus(s);
            MemoryBlock argPointingBlock = null;

            // argが指しているメモリ領域を探す
            switch (s.variableManager().getVariableStatus(arg)) {
            case POINTING:
                Address value = s.variableManager().get(arg).value();
                if (value instanceof HeapAddress) {
                    HeapAddress heapAddress = (HeapAddress) value;
                    argPointingBlock = newStatus.memoryManager().find(heapAddress.memoryBlockId());
                }
                break;
            }

            MemoryBlock b = newStatus.memoryManager().allocate();
            result.add(new MallocEvalElement(newStatus, new HeapAddress(b.id())));
            if (argPointingBlock != null) {
                newStatus.memoryManager().release(argPointingBlock);
            }
        }

        return result;
    }

    private Set<MemoryStatus> analyzeVariableAssign(
            List<IASTNode> pathToVariableAssign, Set<MemoryStatus> entry) {

        // 変数代入のノードから上方向へトラバース
        ListIterator<IASTNode> it = pathToVariableAssign.listIterator(pathToVariableAssign.size());
        IASTNode node = it.previous();

        // pathToVariableAssignの一番後ろの要素は識別子でなければならない
        assert node instanceof IASTBinaryExpression
            && (((IASTBinaryExpression) node).getOperand2() instanceof IASTIdExpression
                    || ((IASTBinaryExpression) node).getOperand2().getRawSignature().equals("NULL"));

        IASTBinaryExpression be = (IASTBinaryExpression) node;
        IASTName lhsName = Util.getName(be.getOperand1());
        IBinding lhsBinding = lhsName == null ? null : lhsName.resolveBinding();

        if (lhsBinding instanceof IVariable) {
            IASTName rhsName = Util.getName(be.getOperand2());
            IBinding rhsBinding = rhsName == null ? null : rhsName.resolveBinding();
            Set<MemoryStatus> intermediateStatus = null;

            if (be.getOperand2().getRawSignature().equals("NULL")) {
                // hoge = NULL
                intermediateStatus = evalAssignNullToVariable(be, entry);
            } else if (rhsBinding instanceof IVariable) {
                // hoge = variable
                intermediateStatus = evalAssignVariableToVariable(be, (IVariable) rhsBinding, entry);
            } else {
                System.out.println("Not supported syntax: the most right expression is not a ID expression");
                return entry;
            }

            // a = b = .. = foo
            // という形なら=の続く限り解析し、最終的な状態を全体の状態とする
            rhsBinding = lhsBinding;
            while (it.hasPrevious() && rhsBinding != null) {
                node = it.previous();
                if (Util.isIASTBinaryExpression(node, IASTBinaryExpression.op_assign)) {
                    be = (IASTBinaryExpression) node;
                    lhsName = Util.getName(be.getOperand1());
                    lhsBinding = lhsName == null ? null : lhsName.resolveBinding();

                    // hoge = foo
                    intermediateStatus = evalAssignVariableToVariable(
                            be, (IVariable) rhsBinding, intermediateStatus);

                    rhsBinding = lhsBinding instanceof IVariable ? (IVariable) lhsBinding : null;
                } else {
                    break;
                }
            }

            if (node == pathToVariableAssign.get(0)) {
                // 最後までトラバースできた場合
                return intermediateStatus;
            } else {
                System.out.println("Not supported syntax");
                return intermediateStatus;
            }
        }

        return entry;
    }

    private static void refIfPointingToHeapAddress(
            IVariable var, MemoryStatus status) {
        VariableManager vm = status.variableManager();
        if (vm.getVariableStatus(var) == Variable.States.POINTING) {
            Address value = vm.get(var).value();
            if (value instanceof HeapAddress) {
                MemoryBlock b = status.memoryManager().find(
                        ((HeapAddress) value).memoryBlockId());
                b.ref();
            }
        }
    }

    private static void unrefIfPointingToHeapAddress(
            IVariable var, MemoryStatus status) {
        VariableManager vm = status.variableManager();
        if (vm.getVariableStatus(var) == Variable.States.POINTING) {
            Address value = vm.get(var).value();
            if (value instanceof HeapAddress) {
                MemoryBlock b = status.memoryManager().find(
                        ((HeapAddress) value).memoryBlockId());
                b.unref();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            String inputFilename = args[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                IASTTranslationUnit translationUnit =
                        new Parser(inputFilename, fileContent).parse();
                Map<String, CFG> procToCFG =
                        new CFGCreator(translationUnit).create();

                for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                    PointToSolver solver =
                            new PointToSolver(entry.getValue(), entry.getValue().entryVertex());
                    Result<CFG.Vertex, MemoryStatus> result = solver.solve();
                    Set<PointToSolver.Problem> problems = solver.problems();

                    for (CFG.Vertex v : Util.sort(result.analysisValue.keySet())) {
                        EntryExitPair<MemoryStatus> memoryStatuses = result.analysisValue.get(v);
                        System.out.println(v.label() + ": exit");
                        for (MemoryStatus memoryStatus : memoryStatuses.exit()) {
                            System.out.println("  " + memoryStatus);

                            for (MemoryBlock b : memoryStatus.memoryManager().memoryBlocks()) {
                                if (b.allocated() && b.refCount() == 0) {
                                    System.out.println("    メモリリーク検出: " + b);
                                }
                            }
                        }

                        for (PointToSolver.Problem p : problems) {
                            if (v.getASTNode().contains(p.position)) {
                                System.out.println("    " + p.message);
                            }
                        }

                        System.out.println();
                    }
                }

            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
