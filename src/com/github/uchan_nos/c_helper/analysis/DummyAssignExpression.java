package com.github.uchan_nos.c_helper.analysis;

import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

public class DummyAssignExpression extends AssignExpression {

    private IASTIdExpression lhs;

    public DummyAssignExpression(int id, IASTIdExpression lhs) {
        super(id, null);
        this.lhs = lhs;
    }

    @Override
    public IASTNode getLHS() {
        return lhs;
    }

    @Override
    public IASTNode getRHS() {
        return null;
    }

}
