package com.github.uchan_nos.c_helper.util;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;

/**
 * すべてのASTノードを統一的に扱うメソッドが追加されたASTVisitor.
 * @author uchan
 *
 */
public class AllASTVisitor extends ASTVisitor{
    public AllASTVisitor() {
        super();
    }
    public AllASTVisitor(boolean visitNodes) {
        super(visitNodes);
    }

    /**
     * すべてのvisit呼び出しを集約したメソッド.
     * @param node 訪問しているノード
     */
    protected void visitAny(IASTNode node) {
    }
    @Override
    public int visit(IASTStatement statement) {
        visitAny(statement);
        return super.visit(statement);
    }
    @Override
    public int visit(IASTDeclaration declaration) {
        visitAny(declaration);
        return super.visit(declaration);
    }
    @Override
    public int visit(IASTTranslationUnit tu) {
        visitAny(tu);
        return super.visit(tu);
    }
    @Override
    public int visit(IASTName name) {
        visitAny(name);
        return super.visit(name);
    }
    @Override
    public int visit(IASTInitializer initializer) {
        visitAny(initializer);
        return super.visit(initializer);
    }
    @Override
    public int visit(IASTParameterDeclaration parameterDeclaration) {
        visitAny(parameterDeclaration);
        return super.visit(parameterDeclaration);
    }
    @Override
    public int visit(IASTDeclarator declarator) {
        visitAny(declarator);
        return super.visit(declarator);
    }
    @Override
    public int visit(IASTDeclSpecifier declSpec) {
        visitAny(declSpec);
        return super.visit(declSpec);
    }
    @Override
    public int visit(IASTArrayModifier arrayModifier) {
        visitAny(arrayModifier);
        return super.visit(arrayModifier);
    }
    @Override
    public int visit(IASTPointerOperator ptrOperator) {
        visitAny(ptrOperator);
        return super.visit(ptrOperator);
    }
    @Override
    public int visit(IASTAttribute attribute) {
        visitAny(attribute);
        return super.visit(attribute);
    }
    @Override
    public int visit(IASTToken token) {
        visitAny(token);
        return super.visit(token);
    }
    @Override
    public int visit(IASTExpression expression) {
        visitAny(expression);
        return super.visit(expression);
    }
    @Override
    public int visit(IASTTypeId typeId) {
        visitAny(typeId);
        return super.visit(typeId);
    }
    @Override
    public int visit(IASTEnumerator enumerator) {
        visitAny(enumerator);
        return super.visit(enumerator);
    }
    @Override
    public int visit(IASTProblem problem) {
        visitAny(problem);
        return super.visit(problem);
    }

    /**
     * すべてのleave呼び出しを集約したメソッド.
     * @param node 離れようとしているノード
     */
    protected void leaveAny(IASTNode node) {
    }
    @Override
    public int leave(IASTStatement statement) {
        leaveAny(statement);
        return super.leave(statement);
    }
    @Override
    public int leave(IASTDeclaration declaration) {
        leaveAny(declaration);
        return super.leave(declaration);
    }
    @Override
    public int leave(IASTTranslationUnit tu) {
        leaveAny(tu);
        return super.leave(tu);
    }
    @Override
    public int leave(IASTName name) {
        leaveAny(name);
        return super.leave(name);
    }
    @Override
    public int leave(IASTInitializer initializer) {
        leaveAny(initializer);
        return super.leave(initializer);
    }
    @Override
    public int leave(IASTParameterDeclaration parameterDeclaration) {
        leaveAny(parameterDeclaration);
        return super.leave(parameterDeclaration);
    }
    @Override
    public int leave(IASTDeclarator declarator) {
        leaveAny(declarator);
        return super.leave(declarator);
    }
    @Override
    public int leave(IASTDeclSpecifier declSpec) {
        leaveAny(declSpec);
        return super.leave(declSpec);
    }
    @Override
    public int leave(IASTArrayModifier arrayModifier) {
        leaveAny(arrayModifier);
        return super.leave(arrayModifier);
    }
    @Override
    public int leave(IASTPointerOperator ptrOperator) {
        leaveAny(ptrOperator);
        return super.leave(ptrOperator);
    }
    @Override
    public int leave(IASTAttribute attribute) {
        leaveAny(attribute);
        return super.leave(attribute);
    }
    @Override
    public int leave(IASTToken token) {
        leaveAny(token);
        return super.leave(token);
    }
    @Override
    public int leave(IASTExpression expression) {
        leaveAny(expression);
        return super.leave(expression);
    }
    @Override
    public int leave(IASTTypeId typeId) {
        leaveAny(typeId);
        return super.leave(typeId);
    }
    @Override
    public int leave(IASTEnumerator enumerator) {
        leaveAny(enumerator);
        return super.leave(enumerator);
    }
    @Override
    public int leave(IASTProblem problem) {
        leaveAny(problem);
        return super.leave(problem);
    }
 }
