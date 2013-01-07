package com.github.uchan_nos.c_helper.util;

import java.util.ArrayList;

import static org.junit.Assert.*;

import org.eclipse.cdt.core.dom.ast.*;
import org.junit.Before;
import org.junit.Test;

import com.github.uchan_nos.c_helper.analysis.FileInfo;
import com.github.uchan_nos.c_helper.analysis.Parser;

public class TypeUtilTest {
    private IASTTranslationUnit tu = null;
    private ArrayList<IASTDeclaration> declarationsInFile = null;

    private final String source =
        ""
        + "#include <stdio.h>\n"
        + "int main(void)\n"
        + "{\n"
        + "    const char * const volatile p;\n"
        + "    p;\n"
        + "    printf(\"hello\");\n"
        + "}\n"
        ;

    @Before
    public void setUp() throws Exception {
        tu = new Parser(new FileInfo("dummy", false), source).parse();
        declarationsInFile = new ArrayList<IASTDeclaration>();
        for (IASTDeclaration decl : tu.getDeclarations()) {
            if (decl.isPartOfTranslationUnitFile()) {
                declarationsInFile.add(decl);
            }
        }
    }

    @Test
    public void testTU() {
        assertSame(declarationsInFile.size(), 1);
        assertTrue(declarationsInFile.get(0) instanceof IASTFunctionDefinition);
    }

    @Test
    public void testRemoveQualifiers() {
        IASTFunctionDefinition mainFunctionDefinition =
            (IASTFunctionDefinition) declarationsInFile.get(0);
        IASTCompoundStatement body = (IASTCompoundStatement) mainFunctionDefinition.getBody();
        IASTStatement[] statements = body.getStatements();

        assertTrue(statements[2] instanceof IASTExpressionStatement);

        IASTExpression expr = ((IASTExpressionStatement) statements[2]).getExpression();
        assertTrue(expr instanceof IASTFunctionCallExpression);

        IASTFunctionCallExpression fce = (IASTFunctionCallExpression) expr;
        assertSame(fce.getArguments().length, 1);
        assertTrue(fce.getArguments()[0] instanceof IASTLiteralExpression);

        IASTLiteralExpression arg0 = (IASTLiteralExpression) fce.getArguments()[0];
        assertSame(arg0.getKind(), IASTLiteralExpression.lk_string_literal);
        assertTrue(arg0.getExpressionType() instanceof IPointerType);

        IPointerType arg0Type = (IPointerType) arg0.getExpressionType();
        assertTrue(arg0Type.getType() instanceof IQualifierType);
        assertTrue(TypeUtil.removeQualifiers(arg0Type.getType()) instanceof IBasicType);
    }

    @Test
    public void testGetPointerToType() {
        IASTFunctionDefinition mainFunctionDefinition =
            (IASTFunctionDefinition) declarationsInFile.get(0);
        IASTCompoundStatement body = (IASTCompoundStatement) mainFunctionDefinition.getBody();
        IASTStatement[] statements = body.getStatements();

        assertTrue(statements.length > 0);
        assertTrue(statements[1] instanceof IASTExpressionStatement);

        IASTExpression expr = ((IASTExpressionStatement) statements[1]).getExpression();
        assertTrue(expr instanceof IASTIdExpression);
        assertTrue(expr.getExpressionType() instanceof IPointerType);

        IType pointerToType = TypeUtil.getPointerToType(expr.getExpressionType());
        assertTrue(pointerToType instanceof IQualifierType);
        assertTrue(((IQualifierType) pointerToType).getType() instanceof IBasicType);
    }
}
