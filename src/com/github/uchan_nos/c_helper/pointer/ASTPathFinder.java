package com.github.uchan_nos.c_helper.pointer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.util.ASTFilter;
import com.github.uchan_nos.c_helper.util.Util;

public class ASTPathFinder {
    public static List<List<IASTNode>> findPath(IASTNode ast, ASTFilter.Predicate pred) {
        final Finder finder = new Finder(pred);
        finder.find(ast);
        return finder.path();
    }

    private static class Finder {
        // この述語がtrueになるパスを探す
        private ASTFilter.Predicate pred;

        public Finder(ASTFilter.Predicate pred) {
            this.pred = pred;
        }

        // 辿ったノードを順に記録する.
        private ArrayList<IASTNode> currentPath = new ArrayList<IASTNode>();

        // malloc にたどり着くパスの一覧.
        private ArrayList<List<IASTNode>> path = new ArrayList<List<IASTNode>>();

        public List<List<IASTNode>> path() {
            return path;
        }

        public void find(IASTNode ast) {
            this.currentPath.add(ast);

            if (pred.pass(ast)) {
                this.path.add(new ArrayList<IASTNode>(this.currentPath));
            }

            for (IASTNode c : ast.getChildren()) {
                find(c);
            }

            this.currentPath.remove(this.currentPath.size() - 1);
        }
    }

    public static ASTFilter.Predicate createFunctionCallPredicate(final String functionName)
    {
        return new ASTFilter.Predicate() {
            @Override public boolean pass(IASTNode ast) {
                if (ast instanceof IASTFunctionCallExpression) {
                    IASTFunctionCallExpression fce = (IASTFunctionCallExpression) ast;
                    if (fce.getFunctionNameExpression() instanceof IASTIdExpression) {
                        IBinding function = Util.getName(fce.getFunctionNameExpression()).resolveBinding();
                        if (function.getName().equals(functionName)) {
                            // 関数呼び出しを見つけたので、現在の探索パスを保存する
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }
}
