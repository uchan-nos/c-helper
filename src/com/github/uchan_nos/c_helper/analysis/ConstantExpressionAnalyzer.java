package com.github.uchan_nos.c_helper.analysis;

import org.eclipse.cdt.core.dom.ast.IASTExpression;

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

    public ConstantExpressionAnalyzer(CFG cfg, RD<CFG.Vertex> rd) {
        this.cfg = cfg;
        this.rd = rd;
    }

    public Value eval(IASTExpression expression) {
        return null;
    }
}
