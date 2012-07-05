package com.github.uchan_nos.c_helper.analysis;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.*;

public class CFGCreator {
    private IASTTranslationUnit translationUnit;

    public CFGCreator(IASTTranslationUnit translationUnit) {
        this.translationUnit = translationUnit;
    }

    public Map<String, CFG> create() {
        Map<String, CFG> procToCFG =
                new HashMap<String, CFG>();

        // 翻訳単位に含まれるすべての宣言を取得する
        IASTDeclaration[] declarations = translationUnit.getDeclarations();

        // 上から順番に処理する
        for (int i = 0; i < declarations.length; ++i) {
            IASTDeclaration decl = declarations[i];

            // 宣言が関数定義ならばCFGを生成し、procToCFGへ登録する
            if (decl instanceof IASTFunctionDefinition) {
                IASTFunctionDefinition fd = (IASTFunctionDefinition) decl;
                String id = String.valueOf(
                        fd.getDeclarator().getName().getSimpleID());
                FunctionCFGCreator creator = new FunctionCFGCreator(fd);
                CFG cfg = creator.create();
                procToCFG.put(id, cfg);
            }
        }
        return procToCFG;
    }
}
