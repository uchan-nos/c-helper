package com.github.uchan_nos.c_helper.analysis;

import java.math.BigInteger;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;

import com.github.uchan_nos.c_helper.analysis.values.IntegralValue;
import com.github.uchan_nos.c_helper.analysis.values.Value;

/**
 * 与えられた式が定数値を持つか否かを判定する機能を提供する.
 * @author uchan
 */
public class ConstantExpressionAnalyzer {
    public static class Info {
        private boolean isConstant;
        private boolean isUndefined;
        private String message;
        private Value value;

        /**
         * 指定された値をもつ定数値を表す情報を生成.
         * @param value 定数値
         */
        public Info(Value value) {
            this.isConstant = true;
            this.isUndefined = false;
            this.message = null;
            this.value = value;
        }

        /**
         * 指定された原因により未定義になったことを表す情報を生成.
         * @param message 未定義になってしまった原因
         */
        public Info(String message) {
            this.isConstant = false;
            this.isUndefined = true;
            this.message = message;
            this.value = null;
        }

        /**
         * 定数ではないことを表す情報を生成.
         */
        public Info() {
            this.isConstant = false;
            this.isUndefined = false;
            this.message = null;
            this.value = null;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isConstant() {
            return isConstant;
        }

        public boolean isUndefined() {
            return isUndefined;
        }

        public String message() {
            return message;
        }

        public Value value() {
            return value;
        }
    }

    private CFG cfg;
    private RD<CFG.Vertex> rd;
    private AnalysisEnvironment analysisEnvironment;

    public ConstantExpressionAnalyzer(CFG cfg, RD<CFG.Vertex> rd, AnalysisEnvironment env) {
        this.cfg = cfg;
        this.rd = rd;
        this.analysisEnvironment = env;
    }

    public Value eval(IASTExpression expression) {
        class SearchLiteralVisitor extends ASTVisitor {
            private IASTLiteralExpression literalExpression = null;
            public IASTLiteralExpression literalExpression() {
                return literalExpression;
            }
            public SearchLiteralVisitor() {
                super(true);
            }
            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTCastExpression) {
                    ((IASTCastExpression) expression).getOperand().accept(this);
                } else if (expression instanceof IASTLiteralExpression) {
                    literalExpression = (IASTLiteralExpression) expression;
                }
                return super.visit(expression);
            }
        };

        if (expression instanceof IASTBinaryExpression) {
            IASTBinaryExpression be = (IASTBinaryExpression) expression;
            Value lhs = eval(be.getOperand1());
            Value rhs = eval(be.getOperand2());
            if (lhs != null && rhs != null) {
                if (lhs instanceof IntegralValue && rhs instanceof IntegralValue) {
                    IntegralValue lhs_ = (IntegralValue) lhs;
                    IntegralValue rhs_ = (IntegralValue) rhs;
                    return new IntegralValue(lhs_.getValue().multiply(rhs_.getValue()), lhs_.getType(), 0, analysisEnvironment);
                }
            }
        }
        SearchLiteralVisitor searchLiteral = new SearchLiteralVisitor();
        expression.accept(searchLiteral);
        IASTLiteralExpression literalExpression = searchLiteral.literalExpression();
        if (literalExpression != null) {
            if (literalExpression.getKind() == IASTLiteralExpression.lk_integer_constant) {
                long value = Long.parseLong(String.valueOf(literalExpression.getValue()));
                return new IntegralValue(
                        BigInteger.valueOf(value),
                        literalExpression.getExpressionType(), 0, analysisEnvironment);
            }
        }
        return null;
    }
}
