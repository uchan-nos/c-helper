package com.github.uchan_nos.c_helper.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import com.github.uchan_nos.c_helper.util.Util;

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

    public static void main(String[] args) {
        if (args.length == 1) {
            String inputFilename = args[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                IASTTranslationUnit translationUnit =
                        new Parser(new FileInfo(inputFilename, false), fileContent).parse();
                Map<String, CFG> procToCFG =
                        new CFGCreator(translationUnit).create();
                String dot =
                        new CFGPrinter(procToCFG).toDotString();
                System.out.print(dot);
            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
