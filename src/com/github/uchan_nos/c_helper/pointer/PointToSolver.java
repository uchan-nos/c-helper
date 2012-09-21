package com.github.uchan_nos.c_helper.pointer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.CFG.Vertex;
import com.github.uchan_nos.c_helper.analysis.IGraph;

import com.github.uchan_nos.c_helper.dataflow.ForwardSolver;

import com.github.uchan_nos.c_helper.util.Util;

public class PointToSolver extends ForwardSolver<CFG.Vertex, MemoryStatus> {
    private final IASTNode nodeToAnalysis;
    public PointToSolver(IGraph<CFG.Vertex> cfg, CFG.Vertex entryVertex, IASTNode nodeToAnalysis) {
        super(cfg, entryVertex);
        this.nodeToAnalysis = nodeToAnalysis;
    }

    @Override
    protected Set<MemoryStatus> getInitValue() {

        Collection<IASTName> variableNames =
            Util.filter(
                Util.getAllVariableNames(nodeToAnalysis),
                new Util.Predicate<IASTName>() {
                    @Override public boolean calc(IASTName arg) {
                        if (!(arg.resolveBinding() instanceof IParameter)) {
                            return true;
                        }

                        IASTNode n = arg.getParent();
                        while (n != null && !nodeToAnalysis.equals(n) && !(n instanceof IASTSimpleDeclaration)) {
                            n = n.getParent();
                        }
                        if (n instanceof IASTSimpleDeclaration) {
                            return false;
                        }
                        return true;
                    }},
                new ArrayList<IASTName>());

        // 重複を取り除く
        Collection<IBinding> variables =
            Util.map(
                variableNames,
                new Util.Function<IBinding, IASTName>() {
                    @Override public IBinding calc(IASTName arg) {
                        return arg.resolveBinding();
                    }},
                new HashSet<IBinding>());

        System.out.println(variables);

        // ポインタ変数一覧を取得
        return null;
    }

    @Override
    protected Set<MemoryStatus> createDefaultSet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean transfer(Vertex v, Set<MemoryStatus> entry,
            Set<MemoryStatus> result) {
        // TODO Auto-generated method stub
        return false;
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
}
