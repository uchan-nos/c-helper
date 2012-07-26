package com.github.uchan_nos.c_helper.util;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;

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
        /*
        class Visitor extends ASTVisitor {
            private ArrayList<IASTNode> filteredNodes;
            private Predicate filterPred;
            public Visitor(Predicate pred) {
                super(true);
                this.filteredNodes = new ArrayList<IASTNode>();
                this.filterPred = pred;
            }
            private void visitAnyNode(IASTNode node) {
                if (this.filterPred.pass(node)) {
                    this.filteredNodes.add(node);
                }
            }
            public Collection<IASTNode> getFilteredNodes() {
                return this.filteredNodes;
            }
            @Override
            public int visit(IASTStatement statement) {
                visitAnyNode(statement);
                return super.visit(statement);
            }
            @Override
            public int visit(IASTDeclaration declaration) {
                visitAnyNode(declaration);
                return super.visit(declaration);
            }
            @Override
            public int visit(IASTTranslationUnit tu) {
                visitAnyNode(tu);
                return super.visit(tu);
            }
            @Override
            public int visit(IASTName name) {
                visitAnyNode(name);
                return super.visit(name);
            }
            @Override
            public int visit(IASTInitializer initializer) {
                visitAnyNode(initializer);
                // TODO Auto-generated method stub
                return super.visit(initializer);
            }
            @Override
            public int visit(IASTParameterDeclaration parameterDeclaration) {
                visitAnyNode(parameterDeclaration);
                // TODO Auto-generated method stub
                return super.visit(parameterDeclaration);
            }
            @Override
            public int visit(IASTDeclarator declarator) {
                visitAnyNode(declarator);
                // TODO Auto-generated method stub
                return super.visit(declarator);
            }
            @Override
            public int visit(IASTDeclSpecifier declSpec) {
                visitAnyNode(declSpec);
                // TODO Auto-generated method stub
                return super.visit(declSpec);
            }
            @Override
            public int visit(IASTArrayModifier arrayModifier) {
                visitAnyNode(arrayModifier);
                // TODO Auto-generated method stub
                return super.visit(arrayModifier);
            }
            @Override
            public int visit(IASTPointerOperator ptrOperator) {
                visitAnyNode(ptrOperator);
                // TODO Auto-generated method stub
                return super.visit(ptrOperator);
            }
            @Override
            public int visit(IASTAttribute attribute) {
                visitAnyNode(attribute);
                // TODO Auto-generated method stub
                return super.visit(attribute);
            }
            @Override
            public int visit(IASTToken token) {
                visitAnyNode(token);
                // TODO Auto-generated method stub
                return super.visit(token);
            }
            @Override
            public int visit(IASTExpression expression) {
                visitAnyNode(expression);
                // TODO Auto-generated method stub
                return super.visit(expression);
            }
            @Override
            public int visit(IASTTypeId typeId) {
                visitAnyNode(typeId);
                // TODO Auto-generated method stub
                return super.visit(typeId);
            }
            @Override
            public int visit(IASTEnumerator enumerator) {
                visitAnyNode(enumerator);
                // TODO Auto-generated method stub
                return super.visit(enumerator);
            }
            @Override
            public int visit(IASTProblem problem) {
                visitAnyNode(problem);
                // TODO Auto-generated method stub
                return super.visit(problem);
            }
        }
        */
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
