package com.github.uchan_nos.c_helper.analysis;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEqualsInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;

/**
 * 代入式を表すクラス.
 * @author uchan
 */
public class AssignExpression {
    private int id;
    private IASTNode ast;
    public AssignExpression(int id, IASTNode ast) {
        this.ast = null;
        if (ast instanceof IASTBinaryExpression) {
            if (((IASTBinaryExpression)ast).getOperator() == IASTBinaryExpression.op_assign) {
                this.ast = ast;
            }
        } else if (ast instanceof IASTDeclarator) {
            if (((IASTDeclarator)ast).getInitializer() != null) {
                this.ast = ast;
            }
        }
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public IASTNode getAST() {
        return ast;
    }

    public IASTNode getLHS() {
        if (ast instanceof IASTBinaryExpression) {
            return ((IASTBinaryExpression)ast).getOperand1();
        } else if (ast instanceof IASTDeclarator) {
            IASTDeclarator nestedDecl = (IASTDeclarator)ast;
            while (nestedDecl.getNestedDeclarator() != null) {
                nestedDecl = nestedDecl.getNestedDeclarator();
            }
            return nestedDecl.getName();
        }
        return null;
    }

    public IASTNode getRHS() {
        if (ast instanceof IASTBinaryExpression) {
            return ((IASTBinaryExpression)ast).getOperand2();
        } else if (ast instanceof IASTDeclarator) {
            IASTInitializer initializer = ((IASTDeclarator)ast).getInitializer();
            if (initializer instanceof IASTEqualsInitializer) {
                return ((IASTEqualsInitializer)initializer).getInitializerClause();
            } else {
                throw new RuntimeException("initializer must be IASTEqualsInitializer");
            }
        }
        return null;
    }
}
