package com.github.uchan_nos.c_helper.pointer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.*;

import com.github.uchan_nos.c_helper.util.Util;

public class MallocCallFinder {
    public static List<List<IASTNode>> findPathToMalloc(IASTNode ast) {
        final Finder finder = new Finder();
        finder.find(ast);
        return finder.pathToMalloc();
    }

    private static class Finder {
        // 辿ったノードを順に記録する.
        private ArrayList<IASTNode> currentPath = new ArrayList<IASTNode>();

        // malloc にたどり着くパスの一覧.
        private ArrayList<List<IASTNode>> pathToMalloc = new ArrayList<List<IASTNode>>();

        public List<List<IASTNode>> pathToMalloc() {
            return pathToMalloc;
        }

        public void find(IASTNode ast) {
            this.currentPath.add(ast);

            if (ast instanceof IASTFunctionCallExpression) {
                IASTFunctionCallExpression fce = (IASTFunctionCallExpression) ast;
                if (fce.getFunctionNameExpression() instanceof IASTIdExpression) {
                    IBinding function = Util.getName(fce.getFunctionNameExpression()).resolveBinding();
                    if (function.getName().equals("malloc")) {
                        // malloc呼び出しを見つけたので、現在の探索パスを保存する
                        this.pathToMalloc.add(new ArrayList<IASTNode>(this.currentPath));
                    }
                }
            }

            for (IASTNode c : ast.getChildren()) {
                find(c);
            }

            this.currentPath.remove(this.currentPath.size() - 1);
        }
    }
}
