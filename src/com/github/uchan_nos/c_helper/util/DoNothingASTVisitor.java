package com.github.uchan_nos.c_helper.util;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.c.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;

/**
 * 一般的なビジターパターンに適合するノーマルなビジター.
 * @author uchan
 *
 */
public abstract class DoNothingASTVisitor extends ASTVisitor {
    public DoNothingASTVisitor() {
        super(true);
    }

    @Override
    public int visit(IASTStatement statement) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTDeclaration declaration) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTTranslationUnit tu) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTName name) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTInitializer initializer) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTParameterDeclaration parameterDeclaration) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTDeclarator declarator) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTDeclSpecifier declSpec) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTArrayModifier arrayModifier) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTPointerOperator ptrOperator) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTAttribute attribute) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTToken token) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTExpression expression) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTTypeId typeId) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTEnumerator enumerator) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(IASTProblem problem) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(ICPPASTTemplateParameter templateParameter) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(ICASTDesignator designator) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(ICPPASTBaseSpecifier baseSpecifier) {
        return PROCESS_ABORT;
    }
    @Override
    public int visit(ICPPASTCapture capture) {
        return PROCESS_ABORT;
    }

    @Override
    public int leave(IASTStatement statement) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTDeclaration declaration) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTTranslationUnit tu) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTName name) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTInitializer initializer) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTParameterDeclaration parameterDeclaration) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTDeclarator declarator) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTDeclSpecifier declSpec) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTArrayModifier arrayModifier) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTPointerOperator ptrOperator) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTAttribute attribute) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTToken token) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTExpression expression) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTTypeId typeId) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTEnumerator enumerator) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(IASTProblem problem) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(ICPPASTNamespaceDefinition namespaceDefinition) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(ICPPASTTemplateParameter templateParameter) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(ICASTDesignator designator) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(ICPPASTBaseSpecifier baseSpecifier) {
        return PROCESS_ABORT;
    }
    @Override
    public int leave(ICPPASTCapture capture) {
        return PROCESS_ABORT;
    }

}
