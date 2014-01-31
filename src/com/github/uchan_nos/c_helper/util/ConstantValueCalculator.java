package com.github.uchan_nos.c_helper.util;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.analysis.AnalysisEnvironment;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.RD;

public class ConstantValueCalculator {
    private RD<CFG.Vertex> rd; 
    private AnalysisEnvironment assumptions;

    public ConstantValueCalculator() {
        this.rd = null;
        this.assumptions = null;
    }

    public ConstantValueCalculator(RD<CFG.Vertex> rd, AnalysisEnvironment assumptions) {
        this.rd = rd;
        this.assumptions = assumptions;
    }

    public int toInteger(IASTExpression expression, CFG.Vertex currentVertex) throws Exception {
        if (expression instanceof IASTBinaryExpression) {
            IASTBinaryExpression be = (IASTBinaryExpression) expression;
            return toInteger(be.getOperand1(), be.getOperand2(), be.getOperator(), currentVertex);
        } else if (expression instanceof IASTUnaryExpression) {
            IASTUnaryExpression ue = (IASTUnaryExpression) expression;
            return toInteger(ue.getOperand(), ue.getOperator(), currentVertex);
        } else if (expression instanceof IASTLiteralExpression) {
            IASTLiteralExpression le = (IASTLiteralExpression) expression;
            return toInteger(le, currentVertex);
        } else if (expression instanceof IASTTypeIdExpression) {
            IASTTypeIdExpression tie = (IASTTypeIdExpression) expression;
            return toInteger(tie.getTypeId(), tie.getOperator(), currentVertex);
        }
        throw new Exception("Can't calculate the value of '" + expression.getRawSignature() + "' ("
                + expression.getClass().getSimpleName() + ")");
    }

    private int toInteger(IASTExpression operand, int operator, CFG.Vertex currentVertex) throws Exception {
        int result = 0;

        switch (operator) {
        case IASTUnaryExpression.op_plus:
            result = toInteger(operand, currentVertex);
            break;
        case IASTUnaryExpression.op_minus:
            result = -toInteger(operand, currentVertex);
            break;
        case IASTUnaryExpression.op_sizeof:
            System.out.println(operand);
            break;
        default:
            throw new Exception("Can't calculate the value with operator " + operator);
        }

        return result;
    }

    private int toInteger(IASTExpression operand1, IASTExpression operand2, int operator, CFG.Vertex currentVertex) throws Exception {
        int value1 = toInteger(operand1, currentVertex);
        int value2 = toInteger(operand2, currentVertex);
        int result = 0;

        switch (operator) {
        case IASTBinaryExpression.op_plus:
            result = value1 + value2;
            break;
        case IASTBinaryExpression.op_minus:
            result = value1 - value2;
            break;
        case IASTBinaryExpression.op_multiply:
            result = value1 * value2;
            break;
        case IASTBinaryExpression.op_divide:
            result = value1 / value2;
            break;
        default:
            throw new Exception("Can't calculate the value with operator " + operator);
        }

        return result;
    }

    private int toInteger(IASTLiteralExpression expression, CFG.Vertex currentVertex) throws Exception {
        switch (expression.getKind()) {
        case IASTLiteralExpression.lk_integer_constant:
            return Integer.parseInt(String.valueOf(expression.getValue()));
        }
        throw new Exception("Can't calculate the value of '" + expression.getRawSignature() + "'");
    }

    private int toInteger(IASTTypeId typeid, int operator, CFG.Vertex currentVertex) throws Exception {
        if (operator == IASTTypeIdExpression.op_sizeof) {
            return TypeUtil.bytesOfType(typeid.getDeclSpecifier(), assumptions);
        }
        throw new Exception("Can't calculate the value of '" + typeid.getRawSignature() + "'");
    }
}

/*
単位計算みたいな仕組みで、定数値算出に用いた仮定を求める機能が必要。
例えば
int a[10];
のとき sizeof(a) の計算には INT_BIT を用いるが、
sizeof(a) / sizeof(a[0]) の計算には（最終的には）仮定を用いない。
*/
