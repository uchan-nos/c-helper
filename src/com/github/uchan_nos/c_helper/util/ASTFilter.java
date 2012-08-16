package com.github.uchan_nos.c_helper.util;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * 指定された条件でASTの要素をフィルタするクラス.
 * @author uchan
 */
public class ASTFilter {
    public interface Predicate {
        /**
         * フィルタを通過するかどうかを表す値を返す.
         * @param node ASTノード
         * @return フィルタを通過させるならtrue
         */
        boolean pass(IASTNode node);
    }

    private IASTNode ast;

    /**
     * 指定されたASTを対象としたフィルタを生成.
     * @param ast
     */
    public ASTFilter(IASTNode ast) {
        this.ast = ast;
    }

    /**
     * 指定された述語によりフィルタリングを行う.
     * @param pred フィルタを通過させたい要素のみ pass() == true となる述語
     * @return フィルタに通過した要素の集合
     */
    public Collection<IASTNode> filter(Predicate pred) {
      class Visitor extends AllASTVisitor {
            private ArrayList<IASTNode> filteredNodes;
            private Predicate filterPred;
            public Visitor(Predicate pred) {
                super(true);
                this.filteredNodes = new ArrayList<IASTNode>();
                this.filterPred = pred;
            }
            @Override
            protected void visitAny(IASTNode node) {
                super.visitAny(node);
                if (this.filterPred.pass(node)) {
                    this.filteredNodes.add(node);
                }
            }
            public Collection<IASTNode> getFilteredNodes() {
                return this.filteredNodes;
            }
        }
        Visitor visitor = new Visitor(pred);
        this.ast.accept(visitor);
        return visitor.getFilteredNodes();
    }
}
